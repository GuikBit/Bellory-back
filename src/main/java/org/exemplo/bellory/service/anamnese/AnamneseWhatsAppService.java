package org.exemplo.bellory.service.anamnese;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.AgendamentoQuestionario;
import org.exemplo.bellory.model.entity.agendamento.StatusQuestionarioAgendamento;
import org.exemplo.bellory.model.entity.instancia.Instance;
import org.exemplo.bellory.model.entity.instancia.InstanceStatus;
import org.exemplo.bellory.model.event.AgendamentoCriadoEvent;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dispara, via WhatsApp (Evolution API), o convite para o cliente responder os
 * questionários (anamneses) vinculados a um agendamento recém-criado. Atualiza o
 * AgendamentoQuestionario com o resultado do envio (ENVIADO / FALHOU). Se a instância
 * WhatsApp da organização não estiver conectada, mantém o registro como PENDENTE para
 * eventual reenvio manual.
 */
@Service
@Slf4j
public class AnamneseWhatsAppService {

    private static final DateTimeFormatter FMT_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final AgendamentoRepository agendamentoRepository;
    private final InstanceRepository instanceRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${evolution.api.url:https://wa.bellory.com.br}")
    private String evolutionApiUrl;

    @Value("${evolution.api.key:}")
    private String evolutionApiKey;

    @Value("${app.url:https://app.bellory.com.br}")
    private String appUrl;

    public AnamneseWhatsAppService(AgendamentoRepository agendamentoRepository,
                                   InstanceRepository instanceRepository,
                                   RestTemplate restTemplate,
                                   ObjectMapper objectMapper) {
        this.agendamentoRepository = agendamentoRepository;
        this.instanceRepository = instanceRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Reage à criação de um agendamento (após commit da transação principal).
     * Roda assíncrono para não prender o request original. O @Transactional precisa
     * estar no método público que recebe a invocação via proxy do Spring; senão
     * self-invocation faria o @Transactional do método interno ser ignorado.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAgendamentoCriado(AgendamentoCriadoEvent event) {
        try {
            dispararParaAgendamento(event.getAgendamentoId());
        } catch (Exception e) {
            log.error("Erro inesperado ao disparar anamnese WhatsApp para agendamento {}: {}",
                    event.getAgendamentoId(), e.getMessage(), e);
        }
    }

    /**
     * Lógica principal: busca AQs pendentes, valida instância, envia e persiste status.
     * Não anotada com @Transactional — herda a transação do método chamador
     * (onAgendamentoCriado) via proxy do Spring.
     */
    public void dispararParaAgendamento(Long agendamentoId) {
        Agendamento ag = agendamentoRepository.findById(agendamentoId).orElse(null);
        if (ag == null) {
            log.warn("Agendamento {} não encontrado ao disparar anamnese WhatsApp", agendamentoId);
            return;
        }

        List<AgendamentoQuestionario> pendentes = ag.getQuestionarios().stream()
                .filter(aq -> aq.getStatus() == StatusQuestionarioAgendamento.PENDENTE)
                .toList();
        if (pendentes.isEmpty()) {
            return;
        }

        String telefone = normalizarTelefoneBR(ag.getCliente().getTelefone());
        if (telefone == null) {
            log.info("Cliente {} sem telefone válido — anamnese WhatsApp não enviada (agendamento {})",
                    ag.getCliente().getId(), agendamentoId);
            return;
        }

        Long orgId = ag.getOrganizacao().getId();
        Optional<Instance> instanceOpt = instanceRepository.findByOrganizacaoIdAndDeletadoFalse(orgId).stream()
                .filter(i -> i.getStatus() == InstanceStatus.CONNECTED || i.getStatus() == InstanceStatus.OPEN)
                .findFirst();

        if (instanceOpt.isEmpty()) {
            log.info("Sem instância WhatsApp conectada para organização {} — anamnese permanece PENDENTE (agendamento {})",
                    orgId, agendamentoId);
            return;
        }

        Instance instance = instanceOpt.get();
        for (AgendamentoQuestionario aq : pendentes) {
            try {
                String mensagem = montarMensagem(ag, aq);
                enviarTextoEvolution(instance.getInstanceName(), telefone, mensagem);
                aq.setStatus(StatusQuestionarioAgendamento.ENVIADO);
                aq.setDtEnvio(LocalDateTime.now());
                log.info("Anamnese WhatsApp enviada: agendamento={}, questionario={}, telefone={}",
                        agendamentoId, aq.getQuestionario().getId(), telefone);
            } catch (Exception e) {
                log.error("Falha ao enviar anamnese WhatsApp (agendamento={}, questionario={}): {}",
                        agendamentoId, aq.getQuestionario().getId(), e.getMessage());
                aq.setStatus(StatusQuestionarioAgendamento.FALHOU);
            }
        }
        // O save da entidade Agendamento (e cascata para os AQs) é feito ao fim do
        // @Transactional desta thread.
        agendamentoRepository.save(ag);
    }

    private String montarMensagem(Agendamento ag, AgendamentoQuestionario aq) {
        String nomeCliente = primeiroNome(ag.getCliente().getNomeCompleto());
        String nomeOrg = ag.getOrganizacao().getNomeFantasia() != null
                ? ag.getOrganizacao().getNomeFantasia()
                : ag.getOrganizacao().getRazaoSocial();
        String dataHora = ag.getDtAgendamento().format(FMT_DATA_HORA);
        String tituloQ = aq.getQuestionario().getTitulo();
        String link = montarLinkResposta(ag, aq);

        return "Olá, " + nomeCliente + "! 💖\n\n"
                + "Tudo certo com seu agendamento na *" + nomeOrg + "* para *" + dataHora + "* — "
                + "estamos ansiosos para te receber!\n\n"
                + "Para nossa equipe te atender com toda a segurança e carinho que você merece, "
                + "preparamos algumas perguntas rápidas: *" + tituloQ + "*. Leva pouquinho tempo, prometo. 🙌\n\n"
                + "👉 " + link + "\n\n"
                + "Suas respostas ficam salvas e nos ajudam a personalizar seu atendimento. "
                + "Qualquer dúvida, é só responder por aqui!";
    }

    /**
     * Formato da URL pública de resposta (definido pelo front):
     *   {appUrl}/avaliacao/{slug}/{questionarioId}/responder?cliente={id}&agendamento={id}&funcionario={id}
     * O parâmetro funcionario é omitido se o agendamento não tiver nenhum funcionário
     * vinculado (cenário raro mas possível).
     */
    private String montarLinkResposta(Agendamento ag, AgendamentoQuestionario aq) {
        String slug = ag.getOrganizacao().getSlug();
        Long questionarioId = aq.getQuestionario().getId();
        Long clienteId = ag.getCliente().getId();
        Long agendamentoId = ag.getId();

        StringBuilder sb = new StringBuilder()
                .append(appUrl)
                .append("/avaliacao/").append(slug)
                .append('/').append(questionarioId)
                .append("/responder")
                .append("?cliente=").append(clienteId)
                .append("&agendamento=").append(agendamentoId);

        if (ag.getFuncionarios() != null && !ag.getFuncionarios().isEmpty()) {
            sb.append("&funcionario=").append(ag.getFuncionarios().get(0).getId());
        }
        return sb.toString();
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
        log.debug("Evolution sendText status={} body={}", response.getStatusCode(), response.getBody());
    }

    /**
     * Normaliza para o formato esperado pela Evolution API: dígitos com DDI.
     * Telefones brasileiros sem DDI (10 ou 11 dígitos) recebem o prefixo 55.
     * Retorna null se o número não parece ser um celular/fixo válido.
     */
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
