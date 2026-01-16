package org.exemplo.bellory.service;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.config.ConfigAgendamentoDTO;
import org.exemplo.bellory.model.dto.config.ConfigSistemaDTO;
import org.exemplo.bellory.model.entity.config.ConfigAgendamento;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.config.ConfigSistemaRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConfigSistemaService {

    private final ConfigSistemaRepository configSistemaRepository;
    private final OrganizacaoRepository organizacaoRepository;

    @Transactional(readOnly = true)
    public ConfigSistemaDTO buscarConfigPorOrganizacao(Long organizacaoId) {
        ConfigSistema config = configSistemaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada para a organização"));
        return convertToDTO(config);
    }

    @Transactional
    public ConfigSistemaDTO salvarConfigCompleta(ConfigSistemaDTO dto, Long organizacaoId) {
        ConfigSistema config = configSistemaRepository.findByOrganizacaoId(organizacaoId)
                .orElseGet(() -> criarNovaConfig(organizacaoId));

        atualizarConfigSistema(config, dto);

        if (dto.getConfigAgendamento() != null) {
            atualizarConfigAgendamento(config, dto.getConfigAgendamento());
        }

        config.setDtAtualizacao(LocalDateTime.now());
        ConfigSistema saved = configSistemaRepository.save(config);

        return convertToDTO(saved);
    }

    @Transactional
    public ConfigAgendamentoDTO atualizarConfigAgendamento(Long organizacaoId, ConfigAgendamentoDTO dto) {
        ConfigSistema config = configSistemaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada"));

        atualizarConfigAgendamento(config, dto);
        config.setDtAtualizacao(LocalDateTime.now());

        ConfigSistema saved = configSistemaRepository.save(config);
        return convertConfigAgendamentoToDTO(saved.getConfigAgendamento());
    }

    private ConfigSistema criarNovaConfig(Long organizacaoId) {
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));

        return ConfigSistema.builder()
                .organizacao(organizacao)
                .configAgendamento(new ConfigAgendamento())
                .dtCriacao(LocalDateTime.now())
                .build();
    }

    private void atualizarConfigSistema(ConfigSistema config, ConfigSistemaDTO dto) {
        if (dto.getUsaEcommerce() != null) {
            config.setUsaEcommerce(dto.getUsaEcommerce());
        }
        if (dto.getUsaGestaoProdutos() != null) {
            config.setUsaGestaoProdutos(dto.getUsaGestaoProdutos());
        }
        if (dto.getUsaPlanosParaClientes() != null) {
            config.setUsaPlanosParaClientes(dto.getUsaPlanosParaClientes());
        }
        if (dto.getDisparaNotificacoesPush() != null) {
            config.setDisparaNotificacoesPush(dto.getDisparaNotificacoesPush());
        }
        if (dto.getUrlAcesso() != null) {
            config.setUrlAcesso(dto.getUrlAcesso());
        }
    }

    private void atualizarConfigAgendamento(ConfigSistema config, ConfigAgendamentoDTO dto) {
        if (config.getConfigAgendamento() == null) {
            config.setConfigAgendamento(new ConfigAgendamento());
        }

        ConfigAgendamento configAgend = config.getConfigAgendamento();

        if (dto.getToleranciaAgendamento() != null) {
            configAgend.setToleranciaAgendamento(dto.getToleranciaAgendamento());
        }
        if (dto.getMinDiasAgendamento() != null) {
            configAgend.setMinDiasAgendamento(dto.getMinDiasAgendamento());
        }
        if (dto.getMaxDiasAgendamento() != null) {
            configAgend.setMaxDiasAgendamento(dto.getMaxDiasAgendamento());
        }
        if (dto.getCancelamentoCliente() != null) {
            configAgend.setCancelamentoCliente(dto.getCancelamentoCliente());
        }
        if (dto.getTempoCancelamentoCliente() != null) {
            configAgend.setTempoCancelamentoCliente(dto.getTempoCancelamentoCliente());
        }
        if (dto.getAprovarAgendamento() != null) {
            configAgend.setAprovarAgendamento(dto.getAprovarAgendamento());
        }
        if (dto.getAprovarAgendamentoAgente() != null) {
            configAgend.setAprovarAgendamentoAgente(dto.getAprovarAgendamentoAgente());
        }
        if (dto.getOcultarFimSemana() != null) {
            configAgend.setOcultarFimSemana(dto.getOcultarFimSemana());
        }
        if (dto.getOcultarDomingo() != null) {
            configAgend.setOcultarDomingo(dto.getOcultarDomingo());
        }
        if (dto.getCobrarSinal() != null) {
            configAgend.setCobrarSinal(dto.getCobrarSinal());
        }
        if (dto.getPorcentSinal() != null) {
            configAgend.setPorcentSinal(dto.getPorcentSinal());
        }
        if (dto.getCobrarSinalAgente() != null) {
            configAgend.setCobrarSinalAgente(dto.getCobrarSinalAgente());
        }
        if (dto.getPorcentSinalAgente() != null) {
            configAgend.setPorcentSinalAgente(dto.getPorcentSinalAgente());
        }
    }

    private ConfigSistemaDTO convertToDTO(ConfigSistema config) {
        return ConfigSistemaDTO.builder()
                .id(config.getId())
                .organizacaoId(config.getOrganizacao().getId())
                .usaEcommerce(config.isUsaEcommerce())
                .usaGestaoProdutos(config.isUsaGestaoProdutos())
                .usaPlanosParaClientes(config.isUsaPlanosParaClientes())
                .disparaNotificacoesPush(config.isDisparaNotificacoesPush())
                .urlAcesso(config.getUrlAcesso())
                .tenantId(config.getTenantId())
                .configAgendamento(convertConfigAgendamentoToDTO(config.getConfigAgendamento()))
                .build();
    }

    private ConfigAgendamentoDTO convertConfigAgendamentoToDTO(ConfigAgendamento config) {
        if (config == null) return null;

        return ConfigAgendamentoDTO.builder()
                .toleranciaAgendamento(config.getToleranciaAgendamento())
                .minDiasAgendamento(config.getMinDiasAgendamento())
                .maxDiasAgendamento(config.getMaxDiasAgendamento())
                .cancelamentoCliente(config.getCancelamentoCliente())
                .tempoCancelamentoCliente(config.getTempoCancelamentoCliente())
                .aprovarAgendamento(config.getAprovarAgendamento())
                .aprovarAgendamentoAgente(config.getAprovarAgendamentoAgente())
                .ocultarFimSemana(config.getOcultarFimSemana())
                .ocultarDomingo(config.getOcultarDomingo())
                .cobrarSinal(config.getCobrarSinal())
                .porcentSinal(config.getPorcentSinal())
                .cobrarSinalAgente(config.getCobrarSinalAgente())
                .porcentSinalAgente(config.getPorcentSinalAgente())
                .build();
    }
}
