package org.exemplo.bellory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.compra.PagamentoDTO;
import org.exemplo.bellory.model.dto.config.ConfigAgendamentoDTO;
import org.exemplo.bellory.model.dto.config.ConfigSistemaDTO;
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

    // Buscar todas as configurações da organização
    @GetMapping
    public ResponseEntity<ResponseAPI<ConfigSistemaDTO>> getConfiguracao(
            @RequestHeader("organizacao-id") Long organizacaoId) {
        try {
            ConfigSistemaDTO config = configSistemaService.buscarConfigPorOrganizacao(organizacaoId);
//            return ResponseEntity.ok(
//                    ResponseAPI.<ConfigSistemaDTO>builder()
//                            .data(config)
//                            .message("Configurações recuperadas com sucesso")
//                            .build()

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
    public ResponseEntity<ResponseAPI<ConfigSistemaDTO>> putConfigSistema(
            @RequestHeader("organizacao-id") Long organizacaoId,
            @Valid @RequestBody ConfigSistemaDTO configSistemaDTO) {
        try {
            ConfigSistemaDTO saved = configSistemaService.salvarConfigCompleta(configSistemaDTO, organizacaoId);

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
    public ResponseEntity<ResponseAPI<ConfigAgendamentoDTO>> putConfigAgendamento(
            @RequestHeader("organizacao-id") Long organizacaoId,
            @Valid @RequestBody ConfigAgendamentoDTO configAgendamentoDTO) {
        try {
            ConfigAgendamentoDTO updated = configSistemaService.atualizarConfigAgendamento(
                    organizacaoId, configAgendamentoDTO
            );
//            return ResponseEntity.ok(
//                    ResponseAPI.<ConfigAgendamentoDTO>builder()
//                            .data(updated)
//                            .message("Configurações de agendamento atualizadas com sucesso")
//                            .build()
//            );
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

    // Você pode criar métodos similares para cliente, serviço, colaborador
    // quando tiver as respectivas classes de configuração
}