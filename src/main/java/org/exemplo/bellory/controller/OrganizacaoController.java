package org.exemplo.bellory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.organizacao.CreateOrganizacaoDTO;
import org.exemplo.bellory.model.dto.organizacao.OrganizacaoResponseDTO;
import org.exemplo.bellory.model.dto.UpdateOrganizacaoDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.OrganizacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizacao")
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
//    @PostMapping
//    @Operation(
//            summary = "Criar nova organização",
//            description = "Cria uma nova organização no sistema com todos os dados necessários"
//    )
//    @ApiResponses(value = {
//            @ApiResponse(
//                    responseCode = "201",
//                    description = "Organização criada com sucesso",
//                    content = @Content(schema = @Schema(implementation = OrganizacaoResponseDTO.class))
//            ),
//            @ApiResponse(
//                    responseCode = "400",
//                    description = "Dados inválidos ou CNPJ já cadastrado"
//            ),
//            @ApiResponse(
//                    responseCode = "401",
//                    description = "Token inválido ou não fornecido"
//            ),
//            @ApiResponse(
//                    responseCode = "403",
//                    description = "Sem permissão para criar organização"
//            )
//    })
//    public ResponseEntity<ResponseAPI<OrganizacaoResponseDTO>> create( @RequestBody CreateOrganizacaoDTO createDTO) {
//        try {
//            // Validações básicas
//            if (createDTO.getCnpj() == null || createDTO.getCnpj().trim().isEmpty()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
//                                .success(false)
//                                .message("O CNPJ é obrigatório.")
//                                .errorCode(400)
//                                .build());
//            }
//
//            if (createDTO.getRazaoSocial() == null || createDTO.getRazaoSocial().trim().isEmpty()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
//                                .success(false)
//                                .message("A razão social é obrigatória.")
//                                .errorCode(400)
//                                .build());
//            }
//
//            if (createDTO.getNomeFantasia() == null || createDTO.getNomeFantasia().trim().isEmpty()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
//                                .success(false)
//                                .message("O nome fantasia é obrigatório.")
//                                .errorCode(400)
//                                .build());
//            }
//
//            if (createDTO.getEmail() == null || createDTO.getEmail().trim().isEmpty()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
//                                .success(false)
//                                .message("O email é obrigatório.")
//                                .errorCode(400)
//                                .build());
//            }
//
//            OrganizacaoResponseDTO response = organizacaoService.create(createDTO);
//
//            return ResponseEntity.status(HttpStatus.CREATED)
//                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
//                            .success(true)
//                            .message("Organização criada com sucesso.")
//                            .dados(response)
//                            .build());
//
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
//                            .success(false)
//                            .message(e.getMessage())
//                            .errorCode(400)
//                            .build());
//        } catch (Exception e) {
//            log.error("Erro ao criar organização: ", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ResponseAPI.<OrganizacaoResponseDTO>builder()
//                            .success(false)
//                            .message("Ocorreu um erro interno ao criar a organização: " + e.getMessage())
//                            .errorCode(500)
//                            .build());
//        }
//    }

    @PostMapping
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
            boolean existe = organizacaoService.existsByCnpj(cnpj);

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
}
