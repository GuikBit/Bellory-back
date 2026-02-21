package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.organizacao.CreateOrganizacaoDTO;
import org.exemplo.bellory.model.dto.organizacao.OrganizacaoResponseDTO;
import org.exemplo.bellory.model.dto.UpdateOrganizacaoDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.OrganizacaoService;
import org.exemplo.bellory.util.CNPJUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/organizacao")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Organizações", description = "API para gerenciamento de organizações")
public class OrganizacaoController {

    private final OrganizacaoService organizacaoService;

    /**
     * Cria uma nova organização
     * POST /api/organizacao
     */

    @PostMapping
    @Operation(summary = "Criar nova organização")
    public ResponseEntity<ResponseAPI<OrganizacaoResponseDTO>> create(@RequestBody @Valid CreateOrganizacaoDTO createDTO) {

        try {
            OrganizacaoResponseDTO response = organizacaoService.create(createDTO);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
                            .success(true)
                            .message("Organização criada com sucesso.")
                            .dados(response)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao criar organização: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao criar a organização: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Lista todas as organizações ativas
     * GET /api/organizacao
     */
    @GetMapping
    @Operation(
            summary = "Listar todas as organizações",
            description = "Retorna uma lista com todas as organizações ativas do sistema"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de organizações retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = OrganizacaoResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token inválido ou não fornecido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sem permissão para listar organizações"
            )
    })
    public ResponseEntity<ResponseAPI<List<OrganizacaoResponseDTO>>> findAll() {
        try {
            List<OrganizacaoResponseDTO> organizacoes = organizacaoService.findAll();

            if (organizacoes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(ResponseAPI.<List<OrganizacaoResponseDTO>>builder()
                                .success(true)
                                .message("Nenhuma organização encontrada.")
                                .dados(organizacoes)
                                .build());
            }

            return ResponseEntity.ok(ResponseAPI.<List<OrganizacaoResponseDTO>>builder()
                    .success(true)
                    .message("Lista de organizações recuperada com sucesso.")
                    .dados(organizacoes)
                    .build());

        } catch (Exception e) {
            log.error("Erro ao listar organizações: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<OrganizacaoResponseDTO>>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar as organizações: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Busca uma organização por ID
     * GET /api/organizacao/{id}
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar organização por ID",
            description = "Retorna os dados de uma organização específica pelo seu ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Organização encontrada com sucesso",
                    content = @Content(schema = @Schema(implementation = OrganizacaoResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token inválido ou não fornecido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sem permissão para acessar esta organização"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organização não encontrada"
            )
    })
    public ResponseEntity<ResponseAPI<OrganizacaoResponseDTO>> findById(
            @Parameter(description = "ID da organização", required = true)
            @PathVariable Long id) {
        try {
            OrganizacaoResponseDTO organizacao = organizacaoService.findById(id);

            return ResponseEntity.ok(ResponseAPI.<OrganizacaoResponseDTO>builder()
                    .success(true)
                    .message("Organização encontrada com sucesso.")
                    .dados(organizacao)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao buscar organização por ID: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar a organização: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<ResponseAPI<OrganizacaoResponseDTO>> buscarPorId(@PathVariable Long id) {
//        try {
//            OrganizacaoResponseDTO organizacao = organizacaoService.findById(id);
//
//            return ResponseEntity.ok(ResponseAPI.<OrganizacaoResponseDTO>builder()
//                    .success(true)
//                    .message("Organização encontrada com sucesso.")
//                    .dados(organizacao)
//                    .build());
//
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
//                            .success(false)
//                            .message(e.getMessage())
//                            .errorCode(404)
//                            .build());
//        } catch (Exception e) {
//            log.error("Erro ao buscar organização por ID: ", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
//                            .success(false)
//                            .message("Ocorreu um erro interno ao buscar a organização: " + e.getMessage())
//                            .errorCode(500)
//                            .build());
//        }
//    }

    /**
     * Atualiza uma organização
     * PUT /api/organizacao/{id}
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar organização",
            description = "Atualiza os dados de uma organização existente. Apenas os campos fornecidos serão atualizados"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Organização atualizada com sucesso",
                    content = @Content(schema = @Schema(implementation = OrganizacaoResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token inválido ou não fornecido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sem permissão para atualizar esta organização"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organização não encontrada"
            )
    })
//    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ResponseAPI<OrganizacaoResponseDTO>> update(
            @Parameter(description = "ID da organização", required = true)
            @PathVariable @NotBlank long id,
            @Valid @RequestBody UpdateOrganizacaoDTO updateDTO) {
        try {
            OrganizacaoResponseDTO organizacaoAtualizada = organizacaoService.update(id, updateDTO);

            return ResponseEntity.ok(ResponseAPI.<OrganizacaoResponseDTO>builder()
                    .success(true)
                    .message("Organização atualizada com sucesso.")
                    .dados(organizacaoAtualizada)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao atualizar organização: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao atualizar a organização: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Desativa uma organização (soft delete)
     * DELETE /api/organizacao/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Desativar organização",
            description = "Desativa uma organização (soft delete). A organização não é removida do banco de dados"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Organização desativada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token inválido ou não fornecido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sem permissão para desativar esta organização"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organização não encontrada"
            )
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ResponseAPI<Void>> delete(
            @Parameter(description = "ID da organização", required = true)
            @PathVariable @NotBlank long id) {
        try {
            organizacaoService.delete(id);

            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Organização desativada com sucesso.")
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao desativar organização: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao desativar a organização: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Busca organização por CNPJ
     * GET /api/organizacao/cnpj/{cnpj}
     */
    @GetMapping("/cnpj/{cnpj}")
    @Operation(
            summary = "Buscar organização por CNPJ",
            description = "Retorna os dados de uma organização específica pelo seu CNPJ"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Organização encontrada com sucesso",
                    content = @Content(schema = @Schema(implementation = OrganizacaoResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token inválido ou não fornecido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sem permissão para buscar organização"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organização não encontrada"
            )
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ResponseAPI<OrganizacaoResponseDTO>> findByCnpj(
            @Parameter(description = "CNPJ da organização", required = true)
            @PathVariable @NotBlank String cnpj) {
        try {
            OrganizacaoResponseDTO organizacao = organizacaoService.findByCnpj(cnpj);

            return ResponseEntity.ok(ResponseAPI.<OrganizacaoResponseDTO>builder()
                    .success(true)
                    .message("Organização encontrada com sucesso.")
                    .dados(organizacao)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao buscar organização por CNPJ: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar a organização: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Ativa uma organização previamente desativada
     * PATCH /api/organizacao/{id}/ativar
     */
    @PatchMapping("/{id}/ativar")
    @Operation(
            summary = "Ativar organização",
            description = "Ativa uma organização que estava desativada"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Organização ativada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token inválido ou não fornecido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sem permissão para ativar organização"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organização não encontrada"
            )
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ResponseAPI<OrganizacaoResponseDTO>> ativar(
            @Parameter(description = "ID da organização", required = true)
            @PathVariable @NotBlank long id) {
        try {
            UpdateOrganizacaoDTO updateDTO = UpdateOrganizacaoDTO.builder()
                    .ativo(true)
                    .build();

            OrganizacaoResponseDTO organizacao = organizacaoService.update(id, updateDTO);

            return ResponseEntity.ok(ResponseAPI.<OrganizacaoResponseDTO>builder()
                    .success(true)
                    .message("Organização ativada com sucesso.")
                    .dados(organizacao)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao ativar organização: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao ativar a organização: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Verifica se um CNPJ já está cadastrado
     * GET /api/organizacao/verificar-cnpj/{cnpj}
     */
    @GetMapping("/verificar-cnpj/{cnpj}")
    @Operation(
            summary = "Verificar disponibilidade de CNPJ",
            description = "Verifica se um CNPJ já está cadastrado no sistema"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Verificação realizada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token inválido ou não fornecido"
            )
    })

    public ResponseEntity<ResponseAPI<Boolean>> verificarCnpj(
            @Parameter(description = "CNPJ a ser verificado", required = true)
            @PathVariable @NotBlank String cnpj) {
        try {
            // Remove formatação do CNPJ recebido
            String cnpjLimpo = CNPJUtil.removerFormatacao(cnpj);

            // Valida o CNPJ (opcional, mas recomendado)
            if (!CNPJUtil.validarCNPJ(cnpjLimpo)) {
                return ResponseEntity.badRequest()
                        .body(ResponseAPI.<Boolean>builder()
                                .success(false)
                                .message("CNPJ inválido.")
                                .errorCode(400)
                                .build());
            }

            boolean existe = organizacaoService.existsByCnpj(cnpjLimpo);

            return ResponseEntity.ok(ResponseAPI.<Boolean>builder()
                    .success(true)
                    .message(existe ? "CNPJ já cadastrado no sistema." : "CNPJ disponível para cadastro.")
                    .dados(existe)
                    .build());

        } catch (Exception e) {
            log.error("Erro ao verificar CNPJ: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Boolean>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao verificar o CNPJ: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/verificar-username/{username}")
    @Operation(summary = "Verifica se o username está disponível")
    public ResponseEntity<ResponseAPI<Boolean>> verificarUsername(
            @Parameter(description = "Username a ser verificado", required = true)
            @PathVariable @NotBlank String username) {
        try {
            boolean existe = organizacaoService.existsByUsername(username);

            return ResponseEntity.ok(ResponseAPI.<Boolean>builder()
                    .success(true)
                    .message(existe ? "Username já cadastrado no sistema." : "Username disponível para cadastro.")
                    .dados(existe)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Boolean>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao verificar o username: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/verificar-email/{email}")
    @Operation(summary = "Verifica se o email está disponível")
    public ResponseEntity<ResponseAPI<Boolean>> verificarEmail(
            @Parameter(description = "Email a ser verificado", required = true)
            @PathVariable @NotBlank @Email String email) {
        try {
            boolean existe = organizacaoService.existsByEmail(email);

            return ResponseEntity.ok(ResponseAPI.<Boolean>builder()
                    .success(true)
                    .message(existe ? "Email já cadastrado no sistema." : "Email disponível para cadastro.")
                    .dados(existe)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Boolean>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao verificar o email: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/verificar-slug/{slug}")
    @Operation(summary = "Verifica se o slug está disponível")
    public ResponseEntity<ResponseAPI<Boolean>> verificarSlug(
            @Parameter(description = "Slug a ser verificado", required = true)
            @PathVariable @NotBlank String slug) {
        try {
            boolean existe = organizacaoService.existsBySlug(slug);

            return ResponseEntity.ok(ResponseAPI.<Boolean>builder()
                    .success(true)
                    .message(existe ? "Slug já está em uso." : "Slug disponível.")
                    .dados(existe)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Boolean>builder()
                            .success(false)
                            .message("Erro ao verificar slug: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== LOGO ====================

    @PostMapping("/logo")
    @Operation(summary = "Upload de logo da organização")
    public ResponseEntity<ResponseAPI<Map<String, String>>> uploadLogo(
            @RequestBody Map<String, Object> body) {
        try {
            String base64Image = extractBase64FromBody(body);
            Map<String, String> resultado = organizacaoService.uploadLogo(base64Image);
            return ResponseEntity.ok(ResponseAPI.<Map<String, String>>builder()
                    .success(true)
                    .message("Logo atualizada com sucesso.")
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message("Erro ao fazer upload da logo.")
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/logo")
    @Operation(summary = "Obter logo da organização")
    public ResponseEntity<ResponseAPI<Map<String, String>>> getLogo() {
        try {
            Map<String, String> resultado = organizacaoService.getLogo();
            return ResponseEntity.ok(ResponseAPI.<Map<String, String>>builder()
                    .success(true)
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message("Erro ao buscar logo.")
                            .errorCode(500)
                            .build());
        }
    }

    @DeleteMapping("/logo")
    @Operation(summary = "Remover logo da organização")
    public ResponseEntity<ResponseAPI<Void>> deleteLogo() {
        try {
            organizacaoService.deleteLogo();
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Logo removida com sucesso.")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao remover logo.")
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== BANNER ====================

    @PostMapping("/banner")
    @Operation(summary = "Upload de banner da organização")
    public ResponseEntity<ResponseAPI<Map<String, String>>> uploadBanner(
            @RequestBody Map<String, Object> body) {
        try {
            String base64Image = extractBase64FromBody(body);
            Map<String, String> resultado = organizacaoService.uploadBanner(base64Image);
            return ResponseEntity.ok(ResponseAPI.<Map<String, String>>builder()
                    .success(true)
                    .message("Banner atualizado com sucesso.")
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message("Erro ao fazer upload do banner.")
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/banner")
    @Operation(summary = "Obter banner da organização")
    public ResponseEntity<ResponseAPI<Map<String, String>>> getBanner() {
        try {
            Map<String, String> resultado = organizacaoService.getBanner();
            return ResponseEntity.ok(ResponseAPI.<Map<String, String>>builder()
                    .success(true)
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message("Erro ao buscar banner.")
                            .errorCode(500)
                            .build());
        }
    }

    @DeleteMapping("/banner")
    @Operation(summary = "Remover banner da organização")
    public ResponseEntity<ResponseAPI<Void>> deleteBanner() {
        try {
            organizacaoService.deleteBanner();
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Banner removido com sucesso.")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao remover banner.")
                            .errorCode(500)
                            .build());
        }
    }

    private String extractBase64FromBody(Map<String, Object> body) {
        Object file = body.get("file");
        if (file == null) {
            throw new IllegalArgumentException("Campo 'file' é obrigatório.");
        }
        if (file instanceof List<?> list) {
            if (list.isEmpty()) {
                throw new IllegalArgumentException("Campo 'file' não pode ser vazio.");
            }
            return list.get(0).toString();
        }
        return file.toString();
    }
}
