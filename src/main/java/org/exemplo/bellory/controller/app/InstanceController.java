package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.instancia.InstanceByNameDTO;
import org.exemplo.bellory.model.dto.instancia.InstanceCreateDTO;
import org.exemplo.bellory.model.dto.instancia.InstanceDTO;
import org.exemplo.bellory.model.dto.instancia.InstanceUpdateDTO;
import org.exemplo.bellory.model.dto.sendMessage.whatsapp.SendTextMessageDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.InstanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instances")
@Slf4j
@Tag(name = "Instâncias WhatsApp", description = "Gerenciamento de instâncias WhatsApp via Evolution API")
public class InstanceController {

    private final InstanceService instanceService;

    public InstanceController(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    /**
     * Criar nova instância do WhatsApp
     * POST /api/instances
     */
    @Operation(summary = "Criar nova instância WhatsApp")
    @PostMapping
    public ResponseEntity<ResponseAPI<InstanceDTO>> createInstance(
            @Valid @RequestBody InstanceCreateDTO dto) {
        try {
            log.info("Requisição para criar instância: {}", dto.getInstanceName());

            InstanceDTO instance = instanceService.createInstance(dto, false, 0);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<InstanceDTO>builder()
                            .success(true)
                            .message("Instância criada com sucesso. Escaneie o QR Code para conectar.")
                            .dados(instance)
                            .build());

        } catch (IllegalArgumentException e) {
            log.error("Erro de validação ao criar instância: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<InstanceDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());

        } catch (SecurityException e) {
            log.error("Erro de segurança ao criar instância: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<InstanceDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());

        } catch (Exception e) {
            log.error("Erro interno ao criar instância: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<InstanceDTO>builder()
                            .success(false)
                            .message("Erro ao criar instância: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Listar todas as instâncias da organização
     * GET /api/instances
     */
    @Operation(summary = "Listar todas as instâncias")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<InstanceDTO>>> getAllInstances() {
        try {
            log.info("Requisição para listar todas as instâncias");

            List<InstanceDTO> instances = instanceService.getAllInstances();

            if (instances.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(ResponseAPI.<List<InstanceDTO>>builder()
                                .success(true)
                                .message("Nenhuma instância encontrada.")
                                .dados(instances)
                                .build());
            }

            return ResponseEntity.ok(ResponseAPI.<List<InstanceDTO>>builder()
                    .success(true)
                    .message("Instâncias recuperadas com sucesso.")
                    .dados(instances)
                    .build());

        } catch (SecurityException e) {
            log.error("Erro de segurança ao listar instâncias: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<List<InstanceDTO>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());

        } catch (Exception e) {
            log.error("Erro interno ao listar instâncias: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<InstanceDTO>>builder()
                            .success(false)
                            .message("Erro ao listar instâncias: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Buscar instância por ID
     * GET /api/instances/{id}
     */
    @Operation(summary = "Buscar instância por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseAPI<InstanceDTO>> getInstanceById(@PathVariable Long id) {
        try {
            log.info("Requisição para buscar instância ID: {}", id);

            InstanceDTO instance = instanceService.getInstanceById(id);

            return ResponseEntity.ok(ResponseAPI.<InstanceDTO>builder()
                    .success(true)
                    .message("Instância encontrada com sucesso.")
                    .dados(instance)
                    .build());

        } catch (IllegalArgumentException e) {
            log.error("Instância não encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<InstanceDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());

        } catch (SecurityException e) {
            log.error("Erro de segurança ao buscar instância: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<InstanceDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());

        } catch (Exception e) {
            log.error("Erro interno ao buscar instância: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<InstanceDTO>builder()
                            .success(false)
                            .message("Erro ao buscar instância: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Atualizar instância
     * PUT /api/instances/{id}
     */
    @Operation(summary = "Atualizar instância")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<InstanceDTO>> updateInstance(
            @PathVariable Long id,
            @RequestBody InstanceUpdateDTO dto) {
        try {
            log.info("Requisição para atualizar instância ID: {}", id);

            InstanceDTO instance = instanceService.updateInstance(id, dto);

            return ResponseEntity.ok(ResponseAPI.<InstanceDTO>builder()
                    .success(true)
                    .message("Instância atualizada com sucesso.")
                    .dados(instance)
                    .build());

        } catch (IllegalArgumentException e) {
            log.error("Erro ao atualizar instância: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<InstanceDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());

        } catch (SecurityException e) {
            log.error("Erro de segurança ao atualizar instância: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<InstanceDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());

        } catch (Exception e) {
            log.error("Erro interno ao atualizar instância: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<InstanceDTO>builder()
                            .success(false)
                            .message("Erro ao atualizar instância: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Deletar instância
     * DELETE /api/instances/{id}
     */
    @Operation(summary = "Deletar instância")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> deleteInstance(@PathVariable Long id) {
        try {
            log.info("Requisição para deletar instância ID: {}", id);

            instanceService.deleteInstance(id);

            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Instância deletada com sucesso.")
                    .build());

        } catch (IllegalArgumentException e) {
            log.error("Instância não encontrada para deletar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());

        } catch (SecurityException e) {
            log.error("Erro de segurança ao deletar instância: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());

        } catch (Exception e) {
            log.error("Erro interno ao deletar instância: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao deletar instância: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Obter QR Code para conectar WhatsApp
     * GET /api/instances/{id}/qrcode
     */
    @Operation(summary = "Obter QR Code para conectar")
    @GetMapping("/{id}/qrcode")
    public ResponseEntity<ResponseAPI<Map<String, String>>> getQRCode(@PathVariable Long id) {
        try {
            log.info("Requisição para obter QR Code da instância ID: {}", id);

            Map<String, String> qrcode = instanceService.getQRCode(id);

            return ResponseEntity.ok(ResponseAPI.<Map<String, String>>builder()
                    .success(true)
                    .message("QR Code obtido com sucesso. Escaneie para conectar.")
                    .dados(qrcode)
                    .build());

        } catch (IllegalArgumentException e) {
            log.error("Instância não encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());

        } catch (SecurityException e) {
            log.error("Erro de segurança ao obter QR Code: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());

        } catch (Exception e) {
            log.error("Erro interno ao obter QR Code: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message("Erro ao obter QR Code: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Obter status de conexão da instância
     * GET /api/instances/{id}/status
     */
    @Operation(summary = "Obter status de conexão")
    @GetMapping("/{id}/status")
    public ResponseEntity<ResponseAPI<Map<String, Object>>> getConnectionStatus(
            @PathVariable Long id) {
        try {
            log.info("Requisição para obter status da instância ID: {}", id);

            Map<String, Object> status = instanceService.getConnectionStatus(id);

            return ResponseEntity.ok(ResponseAPI.<Map<String, Object>>builder()
                    .success(true)
                    .message("Status da conexão obtido com sucesso.")
                    .dados(status)
                    .build());

        } catch (IllegalArgumentException e) {
            log.error("Instância não encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());

        } catch (SecurityException e) {
            log.error("Erro de segurança ao obter status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());

        } catch (Exception e) {
            log.error("Erro interno ao obter status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message("Erro ao obter status: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Desconectar instância (logout)
     * POST /api/instances/{id}/logout
     */
    @Operation(summary = "Desconectar instância (logout)")
    @PostMapping("/{id}/logout")
    public ResponseEntity<ResponseAPI<Void>> logout(@PathVariable Long id) {
        try {
            log.info("Requisição para desconectar instância ID: {}", id);

            instanceService.logout(id);

            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Instância desconectada com sucesso.")
                    .build());

        } catch (IllegalArgumentException e) {
            log.error("Instância não encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());

        } catch (SecurityException e) {
            log.error("Erro de segurança ao desconectar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());

        } catch (Exception e) {
            log.error("Erro interno ao desconectar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao desconectar instância: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Reiniciar instância
     * POST /api/instances/{id}/restart
     */
    @Operation(summary = "Reiniciar instância")
    @PostMapping("/{id}/restart")
    public ResponseEntity<ResponseAPI<Void>> restart(@PathVariable Long id) {
        try {
            log.info("Requisição para reiniciar instância ID: {}", id);

            instanceService.restart(id);

            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Instância reiniciada com sucesso.")
                    .build());

        } catch (IllegalArgumentException e) {
            log.error("Instância não encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());

        } catch (SecurityException e) {
            log.error("Erro de segurança ao reiniciar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());

        } catch (Exception e) {
            log.error("Erro interno ao reiniciar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao reiniciar instância: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Enviar mensagem de texto
     * POST /api/instances/{id}/send-text
     */
    @Operation(summary = "Enviar mensagem de texto")
    @PostMapping("/{id}/send-text")
    public ResponseEntity<ResponseAPI<Map<String, Object>>> sendTextMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendTextMessageDTO dto) {
        try {
            log.info("Requisição para enviar mensagem pela instância ID: {}", id);

            Map<String, Object> result = instanceService.sendTextMessage(id, dto);

            return ResponseEntity.ok(ResponseAPI.<Map<String, Object>>builder()
                    .success(true)
                    .message("Mensagem enviada com sucesso.")
                    .dados(result)
                    .build());

        } catch (IllegalArgumentException e) {
            log.error("Erro ao enviar mensagem: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());

        } catch (IllegalStateException e) {
            log.error("Instância não está conectada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());

        } catch (SecurityException e) {
            log.error("Erro de segurança ao enviar mensagem: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());

        } catch (Exception e) {
            log.error("Erro interno ao enviar mensagem: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message("Erro ao enviar mensagem: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Buscar instância pelo nome")
    @GetMapping("/by-name/{instanceName}")
    public ResponseEntity<ResponseAPI<InstanceByNameDTO>> getInstanceByName(@PathVariable String instanceName) {
        try {
            log.info("Requisição para buscar instância pelo nome: {}", instanceName);

            InstanceByNameDTO instance = instanceService.getInstanceByNameCustom(instanceName);

            return ResponseEntity.ok(ResponseAPI.<InstanceByNameDTO>builder()
                    .success(true)
                    .message("Instância encontrada com sucesso.")
                    .dados(instance)
                    .build());

        } catch (IllegalArgumentException e) {
            log.error("Instância não encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<InstanceByNameDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());

        } catch (SecurityException e) {
            log.error("Erro de segurança ao buscar instância: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<InstanceByNameDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());

        } catch (Exception e) {
            log.error("Erro interno ao buscar instância: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<InstanceByNameDTO>builder()
                            .success(false)
                            .message("Erro ao buscar instância: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

}
