package org.exemplo.bellory.service.filaespera;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.fila.FilaEsperaTentativa;
import org.exemplo.bellory.model.entity.fila.StatusFilaTentativa;
import org.exemplo.bellory.model.entity.instancia.Instance;
import org.exemplo.bellory.model.entity.instancia.InstanceStatus;
import org.exemplo.bellory.model.event.FilaOfertaCriadaEvent;
import org.exemplo.bellory.model.repository.fila.FilaEsperaTentativaRepository;
import org.exemplo.bellory.model.repository.instance.InstanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dispara via WhatsApp (Evolution API) a oferta de adiantamento criada pela fila.
 * Espelha o padrao do {@code AnamneseWhatsAppService}: AFTER_COMMIT async + nova
 * transacao + early return se nao houver instancia conectada (mantem PENDENTE
 * para reenvio futuro).
 */
@Service
@Slf4j
public class FilaEsperaDispatchService {

    private static final DateTimeFormatter FMT_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final FilaEsperaTentativaRepository tentativaRepository;
    private final InstanceRepository instanceRepository;
    private final RestTemplate restTemplate;

    @Value("${evolution.api.url:https://wa.bellory.com.br}")
    private String evolutionApiUrl;

    @Value("${evolution.api.key:}")
    private String evolutionApiKey;

    public FilaEsperaDispatchService(FilaEsperaTentativaRepository tentativaRepository,
                                     InstanceRepository instanceRepository,
                                     RestTemplate restTemplate) {
        this.tentativaRepository = tentativaRepository;
        this.instanceRepository = instanceRepository;
        this.restTemplate = restTemplate;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOfertaCriada(FilaOfertaCriadaEvent event) {
        try {
            dispatch(event.getTentativaId());
        } catch (Exception e) {
            log.error("[Fila] Erro inesperado no dispatch da tentativa {}: {}",
                    event.getTentativaId(), e.getMessage(), e);
            marcarFalha(event.getTentativaId(), "Erro inesperado: " + e.getMessage());
        }
    }

    private void dispatch(Long tentativaId) {
        FilaEsperaTentativa tentativa = tentativaRepository.findById(tentativaId).orElse(null);
        if (tentativa == null) {
            log.warn("[Fila] Tentativa {} nao encontrada para dispatch", tentativaId);
            return;
        }

        if (tentativa.getStatus() != StatusFilaTentativa.PENDENTE) {
            log.debug("[Fila] Tentativa {} nao esta PENDENTE (status={}) - pulando dispatch",
                    tentativaId, tentativa.getStatus());
            return;
        }

        Agendamento agendamento = tentativa.getAgendamento();
        if (agendamento == null || agendamento.getCliente() == null) {
            log.warn("[Fila] Tentativa {} sem agendamento/cliente - marcando FALHA", tentativaId);
            tentativa.setStatus(StatusFilaTentativa.FALHA);
            tentativa.setDtAtualizacao(LocalDateTime.now());
            tentativaRepository.save(tentativa);
            return;
        }

        String telefone = normalizarTelefoneBR(agendamento.getCliente().getTelefone());
        if (telefone == null) {
            log.info("[Fila] Cliente {} sem telefone valido - tentativa {} marcada FALHA",
                    agendamento.getCliente().getId(), tentativaId);
            tentativa.setStatus(StatusFilaTentativa.FALHA);
            tentativa.setDtAtualizacao(LocalDateTime.now());
            tentativaRepository.save(tentativa);
            return;
        }

        Long orgId = tentativa.getOrganizacao().getId();
        Optional<Instance> instanceOpt = instanceRepository.findByOrganizacaoIdAndDeletadoFalse(orgId).stream()
                .filter(Instance::isAtivo)
                .filter(i -> i.getStatus() == InstanceStatus.CONNECTED || i.getStatus() == InstanceStatus.OPEN)
                .findFirst();

        if (instanceOpt.isEmpty()) {
            log.info("[Fila] Sem instancia WhatsApp conectada para org {} - tentativa {} permanece PENDENTE",
                    orgId, tentativaId);
            return;
        }

        Instance instance = instanceOpt.get();
        String mensagem = montarMensagem(agendamento, tentativa);

        try {
            enviarTextoEvolution(instance.getInstanceName(), telefone, mensagem);
            tentativa.setStatus(StatusFilaTentativa.AGUARDANDO_RESPOSTA);
            tentativa.setDtEnvio(LocalDateTime.now());
            tentativa.setDtAtualizacao(LocalDateTime.now());
            tentativaRepository.save(tentativa);
            log.info("[Fila] Oferta enviada: tentativa={}, agendamento={}, telefone={}",
                    tentativaId, agendamento.getId(), telefone);
        } catch (Exception e) {
            log.error("[Fila] Falha ao enviar oferta da tentativa {}: {}", tentativaId, e.getMessage());
            tentativa.setStatus(StatusFilaTentativa.FALHA);
            tentativa.setDtAtualizacao(LocalDateTime.now());
            tentativaRepository.save(tentativa);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFalha(Long tentativaId, String motivo) {
        tentativaRepository.findById(tentativaId).ifPresent(t -> {
            if (!t.isFinalizada()) {
                t.setStatus(StatusFilaTentativa.FALHA);
                t.setDtAtualizacao(LocalDateTime.now());
                tentativaRepository.save(t);
            }
        });
    }

    private String montarMensagem(Agendamento agendamento, FilaEsperaTentativa tentativa) {
        String nomeCliente = primeiroNome(agendamento.getCliente().getNomeCompleto());
        String nomeOrg = agendamento.getOrganizacao().getNomeFantasia() != null
                ? agendamento.getOrganizacao().getNomeFantasia()
                : agendamento.getOrganizacao().getRazaoSocial();
        String dataOriginal = agendamento.getDtAgendamento().format(FMT_DATA_HORA);
        String dataNova = tentativa.getSlotInicio().format(FMT_DATA_HORA);

        return "Oi, " + nomeCliente + "! 💖\n\n"
                + "Boa noticia: surgiu um horario antes do seu agendamento na *" + nomeOrg + "*.\n\n"
                + "📅 Seu horario atual: *" + dataOriginal + "*\n"
                + "✨ Vaga disponivel: *" + dataNova + "*\n\n"
                + "Quer adiantar? Responda:\n"
                + "*SIM* — quero adiantar\n"
                + "*NAO* — vou manter o original\n\n"
                + "Voce tem 30 minutos para responder. Apos esse tempo, a vaga e oferecida ao proximo da fila.";
    }

    private void enviarTextoEvolution(String instanceName, String telefone, String mensagem) {
        String url = evolutionApiUrl + "/message/sendText/" + instanceName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", evolutionApiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("number", telefone);
        body.put("text", mensagem);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Evolution API retornou " + response.getStatusCode());
        }
        log.debug("[Fila] Evolution sendText status={}", response.getStatusCode());
    }

    private String normalizarTelefoneBR(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 10 || digits.length() == 11) {
            return "55" + digits;
        }
        if (digits.length() == 12 || digits.length() == 13) {
            return digits;
        }
        return null;
    }

    private String primeiroNome(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) return "tudo bem";
        int sp = nomeCompleto.indexOf(' ');
        return sp > 0 ? nomeCompleto.substring(0, sp) : nomeCompleto;
    }
}
