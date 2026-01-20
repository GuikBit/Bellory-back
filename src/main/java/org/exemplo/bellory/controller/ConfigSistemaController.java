package org.exemplo.bellory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.compra.PagamentoDTO;
import org.exemplo.bellory.model.dto.config.*;
import org.exemplo.bellory.model.entity.config.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.ConfigSistemaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuracao")
@RequiredArgsConstructor
public class ConfigSistemaController {

    private final ConfigSistemaService configSistemaService;

    @GetMapping
    public ResponseEntity<ResponseAPI<ConfigSistemaDTO>> getConfiguracao() {
        try {
            ConfigSistemaDTO config = configSistemaService.buscarConfigPorOrganizacao();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ConfigSistemaDTO>builder()
                            .success(true)
                            .message("Pagamento processado com sucesso.")
                            .dados(config)
                            .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ResponseAPI.<ConfigSistemaDTO>builder()
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    // Salvar/Atualizar todas as configurações do sistema
    @PutMapping
    public ResponseEntity<ResponseAPI<ConfigSistemaDTO>> putConfigSistema(@Valid @RequestBody ConfigSistemaDTO configSistemaDTO) {
        try {
            ConfigSistemaDTO saved = configSistemaService.salvarConfigCompleta(configSistemaDTO);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ConfigSistemaDTO>builder()
                            .success(true)
                            .message("Pagamento processado com sucesso.")
                            .dados(saved)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ResponseAPI.<ConfigSistemaDTO>builder()
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    // Atualizar apenas configurações de agendamento
    @PutMapping("/agendamento")
    public ResponseEntity<ResponseAPI<ConfigAgendamentoDTO>> putConfigAgendamento(@Valid @RequestBody ConfigAgendamento configAgendamento) {
        try {
            ConfigAgendamentoDTO updated = configSistemaService.atualizarConfigAgendamento(configAgendamento);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ConfigAgendamentoDTO>builder()
                            .success(true)
                            .message("Pagamento processado com sucesso.")
                            .dados(updated)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ResponseAPI.<ConfigAgendamentoDTO>builder()
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @PutMapping("/servico")
    public ResponseEntity<ResponseAPI<ConfigServicoDTO>> putConfigServico(@Valid @RequestBody ConfigServico configServico) {
        try {
            ConfigServicoDTO updated = configSistemaService.atualizarConfigServico(configServico);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ConfigServicoDTO>builder()
                            .success(true)
                            .message("Pagamento processado com sucesso.")
                            .dados(updated)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ResponseAPI.<ConfigServicoDTO>builder()
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @PutMapping("/cliente")
    public ResponseEntity<ResponseAPI<ConfigClienteDTO>> putConfigCliente(@Valid @RequestBody ConfigCliente configcliente) {
        try {
            ConfigClienteDTO updated = configSistemaService.atualizarConfigCliente(configcliente);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ConfigClienteDTO>builder()
                            .success(true)
                            .message("Pagamento processado com sucesso.")
                            .dados(updated)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ResponseAPI.<ConfigClienteDTO>builder()
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @PutMapping("/colaborador")
    public ResponseEntity<ResponseAPI<ConfigColaboradorDTO>> putConfigServico(@Valid @RequestBody ConfigColaborador configColaborador) {
        try {
            ConfigColaboradorDTO updated = configSistemaService.atualizarConfigColaborador(configColaborador);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ConfigColaboradorDTO>builder()
                            .success(true)
                            .message("Pagamento processado com sucesso.")
                            .dados(updated)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ResponseAPI.<ConfigColaboradorDTO>builder()
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @PutMapping("/notificacao")
    public ResponseEntity<ResponseAPI<ConfigNotificacaoDTO>> putConfigServico(@Valid @RequestBody ConfigNotificacao configNotificacao) {
        try {
            ConfigNotificacaoDTO updated = configSistemaService.atualizarConfigNotificacao(configNotificacao);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ConfigNotificacaoDTO>builder()
                            .success(true)
                            .message("Pagamento processado com sucesso.")
                            .dados(updated)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ResponseAPI.<ConfigNotificacaoDTO>builder()
                            .message(e.getMessage())
                            .build()
            );
        }
    }



    // Você pode criar métodos similares para cliente, serviço, colaborador
    // quando tiver as respectivas classes de configuração
}
