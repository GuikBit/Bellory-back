package org.exemplo.bellory.service.notificacao;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.notificacao.ConfigNotificacaoDTO;
import org.exemplo.bellory.model.entity.notificacao.ConfigNotificacao;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.notificacao.ConfigNotificacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigNotificacaoService {

    private final ConfigNotificacaoRepository repository;
    private final OrganizacaoRepository organizacaoRepository;

    @Transactional(readOnly = true)
    public List<ConfigNotificacaoDTO> listarConfiguracoes() {
        return repository.findConfiguracoesAtivasOrdenadas(getOrganizacaoId())
            .stream().map(ConfigNotificacaoDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<ConfigNotificacaoDTO> listarTodasConfiguracoes() {
        return repository.findByOrganizacaoId(getOrganizacaoId())
            .stream().map(ConfigNotificacaoDTO::new).toList();
    }

    @Transactional
    public ConfigNotificacaoDTO criarConfiguracao(ConfigNotificacaoDTO dto) {
        Long orgId = getOrganizacaoId();

        if (repository.existsByOrganizacaoIdAndTipoAndHorasAntes(orgId, dto.getTipo(), dto.getHorasAntes())) {
            throw new IllegalArgumentException("Ja existe configuracao para " + dto.getTipo() +
                " com " + dto.getHorasAntes() + " horas");
        }

        validarHorasAntes(dto.getTipo(), dto.getHorasAntes());

        Organizacao org = organizacaoRepository.findById(orgId)
            .orElseThrow(() -> new IllegalArgumentException("Organizacao nao encontrada"));

        ConfigNotificacao entity = ConfigNotificacao.builder()
            .organizacao(org)
            .tipo(dto.getTipo())
            .horasAntes(dto.getHorasAntes())
            .ativo(dto.getAtivo() != null ? dto.getAtivo() : true)
            .mensagemTemplate(dto.getMensagemTemplate())
            .build();

        return new ConfigNotificacaoDTO(repository.save(entity));
    }

    /**
     * Salva ou atualiza uma configuracao de notificacao baseada no tipo e horasAntes.
     * Se ja existir configuracao para (orgId, tipo, horasAntes), atualiza.
     * Se nao existir, cria uma nova.
     */
    @Transactional
    public ConfigNotificacaoDTO salvarOuAtualizar(ConfigNotificacaoDTO dto) {
        Long orgId = getOrganizacaoId();

        validarHorasAntes(dto.getTipo(), dto.getHorasAntes());

        // Busca configuracao existente pela chave unica completa (orgId, tipo, horasAntes)
        var existente = repository.findByOrganizacaoIdAndTipoAndHorasAntes(orgId, dto.getTipo(), dto.getHorasAntes());

        if (existente.isPresent()) {
            // Atualiza a configuracao existente
            ConfigNotificacao entity = existente.get();
            entity.setMensagemTemplate(dto.getMensagemTemplate());
            if (dto.getAtivo() != null) {
                entity.setAtivo(dto.getAtivo());
            }
            return new ConfigNotificacaoDTO(repository.save(entity));
        } else {
            // Cria nova configuracao
            Organizacao org = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organizacao nao encontrada"));

            ConfigNotificacao entity = ConfigNotificacao.builder()
                .organizacao(org)
                .tipo(dto.getTipo())
                .horasAntes(dto.getHorasAntes())
                .ativo(dto.getAtivo() != null ? dto.getAtivo() : true)
                .mensagemTemplate(dto.getMensagemTemplate())
                .build();

            return new ConfigNotificacaoDTO(repository.save(entity));
        }
    }

    @Transactional
    public ConfigNotificacaoDTO atualizarConfiguracao(Long id, ConfigNotificacaoDTO dto) {
        ConfigNotificacao entity = findByIdAndValidate(id);

        if (!entity.getTipo().equals(dto.getTipo()) || !entity.getHorasAntes().equals(dto.getHorasAntes())) {
            if (repository.existsByOrganizacaoIdAndTipoAndHorasAntes(
                    entity.getOrganizacao().getId(), dto.getTipo(), dto.getHorasAntes())) {
                throw new IllegalArgumentException("Ja existe configuracao para essa combinacao");
            }
            validarHorasAntes(dto.getTipo(), dto.getHorasAntes());
        }

        entity.setTipo(dto.getTipo());
        entity.setHorasAntes(dto.getHorasAntes());
        entity.setAtivo(dto.getAtivo());
        entity.setMensagemTemplate(dto.getMensagemTemplate());

        return new ConfigNotificacaoDTO(repository.save(entity));
    }

    @Transactional
    public void deletarConfiguracao(Long id) {
        repository.delete(findByIdAndValidate(id));
    }

    @Transactional
    public ConfigNotificacaoDTO alterarStatus(Long id, boolean ativo) {
        ConfigNotificacao entity = findByIdAndValidate(id);
        entity.setAtivo(ativo);
        return new ConfigNotificacaoDTO(repository.save(entity));
    }

    private ConfigNotificacao findByIdAndValidate(Long id) {
        ConfigNotificacao entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuracao nao encontrada"));
        if (!entity.getOrganizacao().getId().equals(getOrganizacaoId())) {
            throw new SecurityException("Acesso negado");
        }
        return entity;
    }

    private void validarHorasAntes(TipoNotificacao tipo, Integer horas) {
        List<Integer> permitidos = switch (tipo) {
            case CONFIRMACAO -> List.of(12, 24, 36, 48);
            case LEMBRETE -> List.of(1, 2, 3, 4, 5, 6);
        };
        if (!permitidos.contains(horas)) {
            throw new IllegalArgumentException("Horas invalidas para " + tipo + ". Permitido: " + permitidos);
        }
    }

    private Long getOrganizacaoId() {
        Long orgId = TenantContext.getCurrentOrganizacaoId();
        if (orgId == null) throw new SecurityException("Organizacao nao identificada");
        return orgId;
    }
}
