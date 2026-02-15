package org.exemplo.bellory.service;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.config.*;
import org.exemplo.bellory.model.entity.config.*;
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
    public ConfigSistemaDTO buscarConfigPorOrganizacao() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        ConfigSistema config = configSistemaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada para a organização"));
        return convertToDTO(config);
    }

    @Transactional
    public ConfigSistemaDTO salvarConfigCompleta(ConfigSistemaDTO dto) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

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
    public ConfigAgendamentoDTO atualizarConfigAgendamento(ConfigAgendamento dto) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        ConfigSistema config = configSistemaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada"));

        atualizarConfigAgendamento(config, dto);
        config.setDtAtualizacao(LocalDateTime.now());

        ConfigSistema saved = configSistemaRepository.save(config);
        return convertConfigAgendamentoToDTO(saved.getConfigAgendamento());
    }


    @Transactional
    public ConfigServicoDTO atualizarConfigServico(ConfigServico dto) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        ConfigSistema config = configSistemaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada"));

        atuaConfigServico(config, dto);
        config.setDtAtualizacao(LocalDateTime.now());

        ConfigSistema saved = configSistemaRepository.save(config);
        return convertConfigServicoToDTO(saved.getConfigServico());
    }

    @Transactional
    public ConfigClienteDTO atualizarConfigCliente(ConfigCliente dto) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        ConfigSistema config = configSistemaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada"));

        atuaConfigCliente(config, dto);
        config.setDtAtualizacao(LocalDateTime.now());

        ConfigSistema saved = configSistemaRepository.save(config);
        return convertConfigClienteToDTO(saved.getConfigCliente());
    }

    @Transactional
    public ConfigColaboradorDTO atualizarConfigColaborador(ConfigColaborador dto) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        ConfigSistema config = configSistemaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada"));

        atuaConfigColaborador(config, dto);
        config.setDtAtualizacao(LocalDateTime.now());

        ConfigSistema saved = configSistemaRepository.save(config);
        return convertConfigColaboradorToDTO(saved.getConfigColaborador());
    }

    @Transactional
    public ConfigNotificacaoDTO atualizarConfigNotificacao(ConfigNotificacao dto) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        ConfigSistema config = configSistemaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada"));

        atuaConfigNotificacao(config, dto);
        config.setDtAtualizacao(LocalDateTime.now());

        ConfigSistema saved = configSistemaRepository.save(config);
        return convertConfigNotificacaoToDTO(saved.getConfigNotificacao());
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

    private void atuaConfigServico (ConfigSistema config, ConfigServico dto){
        if(config.getConfigServico() == null){
            config.setConfigServico(new ConfigServico());
        }

        ConfigServico configServ = config.getConfigServico();

        if(dto.getUnicoServicoAgendamento() != null){
            configServ.setUnicoServicoAgendamento(dto.getUnicoServicoAgendamento());
        }
        if(dto.getMostrarValorAgendamento() != null){
            configServ.setMostrarAvaliacao(dto.getMostrarAvaliacao());
        }
        if(dto.getMostrarAvaliacao() != null){
            configServ.setMostrarAvaliacao(dto.getMostrarAvaliacao());
        }
    }

    private void atuaConfigCliente (ConfigSistema config, ConfigCliente dto){
        if(config.getConfigCliente() == null){
            config.setConfigCliente(new ConfigCliente());
        }

        ConfigCliente configServ = config.getConfigCliente();

        if(dto.getPrecisaCadastroAgendar() != null){
            configServ.setPrecisaCadastroAgendar(dto.getPrecisaCadastroAgendar());
        }
        if(dto.getProgramaFidelidade() != null){
            configServ.setProgramaFidelidade(dto.getProgramaFidelidade());
        }
        if(dto.getValorGastoUmPonto() != null){
            configServ.setValorGastoUmPonto(dto.getValorGastoUmPonto());
        }
    }

    private void atuaConfigColaborador (ConfigSistema config, ConfigColaborador dto){
        if(config.getConfigColaborador() == null){
            config.setConfigColaborador(new ConfigColaborador());
        }

        ConfigColaborador configServ = config.getConfigColaborador();

        if(dto.getSelecionarColaboradorAgendamento() != null){
            configServ.setSelecionarColaboradorAgendamento(dto.getSelecionarColaboradorAgendamento());
        }
        if(dto.getMostrarNotasComentarioColaborador() != null){
            configServ.setMostrarNotasComentarioColaborador(dto.getMostrarNotasComentarioColaborador());
        }
        if(dto.getComissaoPadrao() != null){
            configServ.setComissaoPadrao(dto.getComissaoPadrao());
        }
    }

    private void atuaConfigNotificacao (ConfigSistema config, ConfigNotificacao dto){
        if(config.getConfigNotificacao() == null){
            config.setConfigNotificacao(new ConfigNotificacao());
        }

        ConfigNotificacao configServ = config.getConfigNotificacao();

        if(dto.getEnviarConfirmacaoWhatsapp() != null){
            configServ.setEnviarConfirmacaoWhatsapp(dto.getEnviarConfirmacaoWhatsapp());
        }
        if(dto.getEnviarLembreteWhatsapp() != null){
            configServ.setEnviarLembreteWhatsapp(dto.getEnviarLembreteWhatsapp());
        }
        if(dto.getEnviarLembreteSMS() != null){
            configServ.setEnviarLembreteSMS(dto.getEnviarLembreteSMS());
        }
        if(dto.getEnviarLembreteEmail() != null){
            configServ.setEnviarLembreteEmail(dto.getEnviarLembreteEmail());
        }
        if(dto.getEnviarConfirmacaoForaHorario() != null){
            configServ.setEnviarConfirmacaoForaHorario(dto.getEnviarConfirmacaoForaHorario());
        }
        if(dto.getTempoParaConfirmacao() != null){
            configServ.setTempoParaConfirmacao(dto.getTempoParaConfirmacao());
        }
        if(dto.getTempoLembretePosConfirmacao() != null){
            configServ.setTempoLembretePosConfirmacao(dto.getTempoLembretePosConfirmacao());
        }

        if(dto.getMensagemTemplateConfirmacao() != null){
            configServ.setMensagemTemplateConfirmacao(dto.getMensagemTemplateConfirmacao());
        }
        if(dto.getMensagemTemplateLembrete() != null){
            configServ.setMensagemTemplateLembrete(dto.getMensagemTemplateLembrete());
        }
    }

    private void atualizarConfigAgendamento(ConfigSistema config, ConfigAgendamento dto) {
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
        if(dto.getMostrarAgendamentoCancelado() != null){
            configAgend.setMostrarAgendamentoCancelado(dto.getMostrarAgendamentoCancelado());
        }

        if(dto.getModoVizualizacao() != null){
            configAgend.setModoVizualizacao(dto.getModoVizualizacao());
        }
        // Segundo bloco - Cancelamento Cliente
        if (dto.getCancelamentoCliente() != null) {
            configAgend.setCancelamentoCliente(dto.getCancelamentoCliente());
            if (dto.getCancelamentoCliente().equals(false)) {
                configAgend.setTempoCancelamentoCliente(null);
            }
        }
        // Só atualiza o tempo se cancelamentoCliente for true (ou null)
        if (dto.getTempoCancelamentoCliente() != null && !Boolean.FALSE.equals(dto.getCancelamentoCliente())) {
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
        // Primeiro bloco - Cobrar Sinal
        if (dto.getCobrarSinal() != null) {
            configAgend.setCobrarSinal(dto.getCobrarSinal());
            if (dto.getCobrarSinal().equals(false)) {
                configAgend.setPorcentSinal(null);
            }
        }
        // Só atualiza a porcentagem se cobrarSinal for true (ou null, mantendo o valor existente)
        if (dto.getPorcentSinal() != null && !Boolean.FALSE.equals(dto.getCobrarSinal())) {
            configAgend.setPorcentSinal(dto.getPorcentSinal());
        }

        if (dto.getCobrarSinalAgente() != null) {
            configAgend.setCobrarSinalAgente(dto.getCobrarSinalAgente());
            if (dto.getCobrarSinalAgente().equals(false)) {
                configAgend.setPorcentSinalAgente(null);
            }
        }
        // Só atualiza a porcentagem se cobrarSinalAgente for true (ou null)
        if (dto.getPorcentSinalAgente() != null && !Boolean.FALSE.equals(dto.getCobrarSinalAgente())) {
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
                .configAgendamento(config.getConfigAgendamento())
                .configServico(config.getConfigServico())
                .configCliente(config.getConfigCliente())
                .configColaborador(config.getConfigColaborador())
                .configNotificacao(config.getConfigNotificacao())
                .build();
    }
    private ConfigServicoDTO convertConfigServicoToDTO(ConfigServico config) {
        if (config == null) return null;

        return ConfigServicoDTO.builder()
                .mostrarAvaliacao(config.getMostrarAvaliacao())
                .unicoServicoAgendamento(config.getUnicoServicoAgendamento())
                .mostrarValorAgendamento(config.getMostrarValorAgendamento())
                .build();
    }

    private ConfigClienteDTO convertConfigClienteToDTO(ConfigCliente config) {
        if (config == null) return null;

        return ConfigClienteDTO.builder()
                .programaFidelidade(config.getProgramaFidelidade())
                .programaFidelidade(config.getProgramaFidelidade())
                .valorGastoUmPonto(config.getValorGastoUmPonto())
                .build();
    }

    private ConfigColaboradorDTO convertConfigColaboradorToDTO(ConfigColaborador config) {
        if (config == null) return null;

        return ConfigColaboradorDTO.builder()
                .selecionarColaboradorAgendamento(config.getSelecionarColaboradorAgendamento())
                .mostrarNotasComentarioColaborador(config.getMostrarNotasComentarioColaborador())
                .comissaoPadrao(config.getComissaoPadrao())
                .build();
    }

    private ConfigNotificacaoDTO convertConfigNotificacaoToDTO(ConfigNotificacao config) {
        if (config == null) return null;

        return ConfigNotificacaoDTO.builder()
                .enviarLembreteWhatsapp(config.getEnviarLembreteWhatsapp())
                .enviarLembreteSMS(config.getEnviarLembreteSMS())
                .enviarLembreteEmail(config.getEnviarLembreteEmail())
                .enviarConfirmacaoForaHorario(config.getEnviarConfirmacaoForaHorario())
                .tempoParaConfirmacao(config.getTempoParaConfirmacao())
                .tempoLembretePosConfirmacao(config.getTempoLembretePosConfirmacao())
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
