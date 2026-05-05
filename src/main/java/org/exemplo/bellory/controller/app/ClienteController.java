package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.exemplo.bellory.model.dto.*;
import org.exemplo.bellory.model.dto.clienteDTO.*;
import org.exemplo.bellory.model.dto.compra.CompraDTO;
import org.exemplo.bellory.model.dto.compra.CompraFiltroDTO;
import org.exemplo.bellory.model.dto.compra.PagamentoDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.ClienteService;
import org.exemplo.bellory.service.cliente.ClienteImportacaoService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cliente")
@Tag(name = "Clientes", description = "Gerenciamento de clientes, histórico, agendamentos e estatísticas")
public class ClienteController {

    private final ClienteService clienteService;
    private final ClienteImportacaoService clienteImportacaoService;

    public ClienteController(ClienteService clienteService,
                             ClienteImportacaoService clienteImportacaoService) {
        this.clienteService = clienteService;
        this.clienteImportacaoService = clienteImportacaoService;
    }

    // =============== CRUD BÁSICO ===============

    /**
     * Listar todos os clientes com filtros
     */
    @Operation(summary = "Listar clientes com filtros")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<ClienteDTO>>> getClientes(
            @RequestParam(defaultValue = "nomeCompleto") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String telefone,
            @RequestParam(required = false) Boolean ativo) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            ClienteFiltroDTO filtro = ClienteFiltroDTO.builder()
                    .nome(nome)
                    .email(email)
                    .telefone(telefone)
                    .ativo(ativo)
                    .build();

            List<ClienteDTO> clientes = clienteService.getClientesComFiltros(filtro, sort);

            return ResponseEntity.ok(ResponseAPI.<List<ClienteDTO>>builder()
                    .success(true)
                    .message("Lista de clientes recuperada com sucesso.")
                    .dados(clientes)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ClienteDTO>>builder()
                            .success(false)
                            .message("Erro interno ao buscar clientes: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Buscar cliente por ID
     */
    @Operation(summary = "Buscar cliente por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseAPI<ClienteDetalhadoDTO>> getClienteById(@PathVariable Long id) {
        try {
            ClienteDetalhadoDTO cliente = clienteService.getClienteDetalhadoById(id);

            return ResponseEntity.ok(ResponseAPI.<ClienteDetalhadoDTO>builder()
                    .success(true)
                    .message("Cliente encontrado com sucesso.")
                    .dados(cliente)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ClienteDetalhadoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ClienteDetalhadoDTO>builder()
                            .success(false)
                            .message("Erro interno ao buscar cliente: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Criar novo cliente
     */
    @Operation(summary = "Criar novo cliente")
    @PostMapping
    public ResponseEntity<ResponseAPI<ClienteDTO>> createCliente(@RequestBody @Valid ClienteCreateDTO clienteCreateDTO) {
        try {
            ClienteDTO novoCliente = clienteService.createCliente(clienteCreateDTO);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ClienteDTO>builder()
                            .success(true)
                            .message("Cliente criado com sucesso.")
                            .dados(novoCliente)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<ClienteDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ClienteDTO>builder()
                            .success(false)
                            .message("Erro interno ao criar cliente: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // =============== IMPORTACAO EM MASSA VIA CSV ===============

    /**
     * Inicia uma importacao de clientes em massa via CSV.
     * Retorna imediatamente com o {@code importId}; o processamento corre em background.
     * Acompanhe via {@code GET /importar-csv/{id}} ate {@code status = CONCLUIDO|FALHA}.
     */
    @Operation(summary = "Importar clientes em massa via CSV (assincrono)")
    @PostMapping(value = "/importar-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseAPI<ImportacaoStatusDTO>> importarCsv(
            @RequestParam("file") MultipartFile file) {
        try {
            ImportacaoStatusDTO status = clienteImportacaoService.iniciar(file);
            return ResponseEntity.accepted()
                    .body(ResponseAPI.<ImportacaoStatusDTO>builder()
                            .success(true)
                            .message("Importacao iniciada. Acompanhe pelo importId.")
                            .dados(status)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseAPI.<ImportacaoStatusDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ImportacaoStatusDTO>builder()
                            .success(false)
                            .message("Erro interno ao iniciar importacao: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Lista todas as importacoes da organizacao logada (mais recentes primeiro).
     * Usada na tela de historico — quando o usuario clica em uma importacao, o
     * frontend faz polling em {@code GET /importar-csv/{id}} para ver detalhes.
     */
    @Operation(summary = "Listar importacoes de clientes da organizacao")
    @GetMapping("/importar-csv")
    public ResponseEntity<ResponseAPI<List<ImportacaoResumoDTO>>> listarImportacoes() {
        try {
            List<ImportacaoResumoDTO> importacoes = clienteImportacaoService.listarImportacoes();
            return ResponseEntity.ok(ResponseAPI.<List<ImportacaoResumoDTO>>builder()
                    .success(true)
                    .message("Importacoes recuperadas.")
                    .dados(importacoes)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ImportacaoResumoDTO>>builder()
                            .success(false)
                            .message("Erro interno ao listar importacoes: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Retorna status atual de uma importacao (progresso e, ao final, lista de erros).
     */
    @Operation(summary = "Consultar status da importacao de clientes")
    @GetMapping("/importar-csv/{id}")
    public ResponseEntity<ResponseAPI<ImportacaoStatusDTO>> getStatusImportacao(@PathVariable Long id) {
        try {
            ImportacaoStatusDTO status = clienteImportacaoService.getStatus(id);
            return ResponseEntity.ok(ResponseAPI.<ImportacaoStatusDTO>builder()
                    .success(true)
                    .message("Status da importacao recuperado.")
                    .dados(status)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ImportacaoStatusDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ImportacaoStatusDTO>builder()
                            .success(false)
                            .message("Erro interno ao consultar importacao: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Download do template CSV oficial (header + linhas de exemplo).
     */
    @Operation(summary = "Baixar template CSV de importacao de clientes")
    @GetMapping(value = "/importar-csv/template", produces = "text/csv")
    public ResponseEntity<ByteArrayResource> baixarTemplateCsv() {
        byte[] conteudo = clienteImportacaoService.gerarTemplateCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"template_clientes.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(conteudo.length)
                .body(new ByteArrayResource(conteudo));
    }

    @Operation(summary = "Validar disponibilidade de username")
    @GetMapping("/validar-username")
    public ResponseEntity<ResponseAPI<UsernameValidationResponseDTO>> validarUsername(@RequestParam String username) {
        try {
            // Limpar e padronizar o username
            String usernameLimpo = username.trim().toLowerCase();

            if (usernameLimpo.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<UsernameValidationResponseDTO>builder()
                                .success(false)
                                .message("Username não pode ser vazio.")
                                .build()
                        );
            }

            boolean existe = clienteService.verificarSeUsernameExiste(usernameLimpo);

            UsernameValidationResponseDTO response = UsernameValidationResponseDTO.builder()
                    .username(usernameLimpo)
                    .disponivel(!existe)
                    .message(existe ? "Username já está em uso" : "Username disponível")
                    .build();

            return ResponseEntity.ok(
                    ResponseAPI.<UsernameValidationResponseDTO>builder()
                            .success(true)
                            .message(existe ? "Username já está em uso" : "Username disponível")
                            .dados(response)
                            .build()
            );

        } catch (Exception e) {
            System.err.println("Erro ao validar username: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<UsernameValidationResponseDTO>builder()
                            .success(false)
                            .message("Erro interno ao validar username.")
                            .build()
                    );
        }
    }

    @Operation(summary = "Verificar se CPF já existe")
    @PostMapping("/verificar-cpf") // Endpoint com nome mais claro
    public ResponseEntity<ResponseAPI<Boolean>> verificarSeCpfExiste(@RequestBody @Valid RequestCpfDTO request) {

        // Chama o serviço para verificar se o CPF já existe
        boolean cpfExiste = clienteService.verificarSeCpfExiste(request.getCpf());

        if (cpfExiste) {
            // Se existe, retorna sucesso na operação, mas com a informação de que já está cadastrado
            return ResponseEntity.ok(ResponseAPI.<Boolean>builder()
                    .success(true)
                    .message("CPF já cadastrado.")
                    .dados(false) // O dado retornado é 'false' (CPF, já cadastrado)
                    .build());
        } else {
            // Se não existe, retorna sucesso com a informação de que está disponível
            return ResponseEntity.ok(ResponseAPI.<Boolean>builder()
                    .success(true)
                    .message("CPF disponível para cadastro.")
                    .dados(true) // O dado retornado é 'true' (CPF, disponivel)
                    .build());
        }
    }

    /**
     * Atualizar cliente
     */
    @Operation(summary = "Atualizar cliente")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<ClienteDTO>> updateCliente(
            @PathVariable Long id,
            @RequestBody ClienteUpdateDTO clienteUpdateDTO) {
        try {
            ClienteDTO clienteAtualizado = clienteService.updateCliente(id, clienteUpdateDTO);

            return ResponseEntity.ok(ResponseAPI.<ClienteDTO>builder()
                    .success(true)
                    .message("Cliente atualizado com sucesso.")
                    .dados(clienteAtualizado)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ClienteDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ClienteDTO>builder()
                            .success(false)
                            .message("Erro interno ao atualizar cliente: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Desativar cliente (soft delete)
     */
    @Operation(summary = "Desativar cliente (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> deleteCliente(@PathVariable Long id) {
        try {
            clienteService.desativarCliente(id);

            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Cliente desativado com sucesso.")
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro interno ao desativar cliente: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // =============== AGENDAMENTOS DO CLIENTE ===============

    /**
     * Listar agendamentos do cliente
     */
    @Operation(summary = "Listar agendamentos do cliente")
    @GetMapping("/{id}/agendamentos")
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> getAgendamentosCliente(
            @PathVariable Long id,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim) {

        try {
            AgendamentoFiltroDTO filtro = AgendamentoFiltroDTO.builder()
                    .clienteId(id)
                    .status(status)
                    .dataInicio(dataInicio != null ? LocalDate.parse(dataInicio) : null)
                    .dataFim(dataFim != null ? LocalDate.parse(dataFim) : null)
                    .build();

            List<AgendamentoDTO> agendamentos = clienteService.getAgendamentosCliente(filtro);

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Agendamentos do cliente recuperados com sucesso.")
                    .dados(agendamentos)
                    .build());

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Formato de data inválido. Use yyyy-MM-dd")
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Erro interno ao buscar agendamentos: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Próximos agendamentos do cliente
     */
    @Operation(summary = "Listar próximos agendamentos do cliente")
    @GetMapping("/{id}/agendamentos/proximos")
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> getProximosAgendamentos(@PathVariable Long id) {
        try {
            List<AgendamentoDTO> proximosAgendamentos = clienteService.getProximosAgendamentosCliente(id);

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Próximos agendamentos recuperados com sucesso.")
                    .dados(proximosAgendamentos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // =============== COMPRAS DO CLIENTE ===============

    /**
     * Listar compras do cliente
     */
    @Operation(summary = "Listar compras do cliente")
    @GetMapping("/{id}/compras")
    public ResponseEntity<ResponseAPI<List<CompraDTO>>> getComprasCliente(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim) {

        try {
            CompraFiltroDTO filtro = CompraFiltroDTO.builder()
                    .clienteId(id)
                    .status(status)
                    .dataInicio(dataInicio != null ? LocalDate.parse(dataInicio) : null)
                    .dataFim(dataFim != null ? LocalDate.parse(dataFim) : null)
                    .build();

            List<CompraDTO> compras = clienteService.getComprasCliente(filtro);

            return ResponseEntity.ok(ResponseAPI.<List<CompraDTO>>builder()
                    .success(true)
                    .message("Compras do cliente recuperadas com sucesso.")
                    .dados(compras)
                    .build());

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseAPI.<List<CompraDTO>>builder()
                            .success(false)
                            .message("Formato de data inválido. Use yyyy-MM-dd")
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<CompraDTO>>builder()
                            .success(false)
                            .message("Erro interno ao buscar compras: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // =============== COBRANÇAS E PAGAMENTOS ===============

    /**
     * Listar cobranças do cliente
     */
    @Operation(summary = "Listar cobranças do cliente")
    @GetMapping("/{id}/cobrancas")
    public ResponseEntity<ResponseAPI<List<CobrancaDTO>>> getCobrancasCliente(
            @PathVariable Long id,
            @RequestParam(required = false) String status) {

        try {
            List<CobrancaDTO> cobrancas = clienteService.getCobrancasCliente(id, status);

            return ResponseEntity.ok(ResponseAPI.<List<CobrancaDTO>>builder()
                    .success(true)
                    .message("Cobranças do cliente recuperadas com sucesso.")
                    .dados(cobrancas)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<CobrancaDTO>>builder()
                            .success(false)
                            .message("Erro interno ao buscar cobranças: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Histórico de pagamentos do cliente
     */
    @Operation(summary = "Listar pagamentos do cliente")
    @GetMapping("/{id}/pagamentos")
    public ResponseEntity<ResponseAPI<List<PagamentoDTO>>> getPagamentosCliente(
            @PathVariable Long id,
            @RequestParam(required = false) String metodo) {

        try {
            List<PagamentoDTO> pagamentos = clienteService.getPagamentosCliente(id, metodo);

            return ResponseEntity.ok(ResponseAPI.<List<PagamentoDTO>>builder()
                    .success(true)
                    .message("Pagamentos do cliente recuperados com sucesso.")
                    .dados(pagamentos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<PagamentoDTO>>builder()
                            .success(false)
                            .message("Erro interno ao buscar pagamentos: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // =============== HISTÓRICO UNIFICADO ===============

    /**
     * Histórico completo do cliente (agendamentos + compras)
     */
    @Operation(summary = "Obter histórico completo do cliente")
    @GetMapping("/{id}/historico")
    public ResponseEntity<ResponseAPI<List<HistoricoClienteDTO>>> getHistoricoCliente(
            @PathVariable Long id,
            @RequestParam(required = false) String tipo, // AGENDAMENTO, COMPRA, TODOS
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim) {

        try {
            HistoricoFiltroDTO filtro = HistoricoFiltroDTO.builder()
                    .clienteId(id)
                    .tipo(tipo)
                    .dataInicio(dataInicio != null ? LocalDate.parse(dataInicio) : null)
                    .dataFim(dataFim != null ? LocalDate.parse(dataFim) : null)
                    .build();

            List<HistoricoClienteDTO> historico = clienteService.getHistoricoCliente(filtro);

            return ResponseEntity.ok(ResponseAPI.<List<HistoricoClienteDTO>>builder()
                    .success(true)
                    .message("Histórico do cliente recuperado com sucesso.")
                    .dados(historico)
                    .build());

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseAPI.<List<HistoricoClienteDTO>>builder()
                            .success(false)
                            .message("Formato de data inválido. Use yyyy-MM-dd")
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<HistoricoClienteDTO>>builder()
                            .success(false)
                            .message("Erro interno ao buscar histórico: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // =============== ESTATÍSTICAS E RESUMOS ===============

    /**
     * Resumo financeiro do cliente
     */
    @Operation(summary = "Obter resumo financeiro do cliente")
    @GetMapping("/{id}/resumo-financeiro")
    public ResponseEntity<ResponseAPI<ResumoFinanceiroClienteDTO>> getResumoFinanceiroCliente(@PathVariable Long id) {
        try {
            ResumoFinanceiroClienteDTO resumo = clienteService.getResumoFinanceiroCliente(id);

            return ResponseEntity.ok(ResponseAPI.<ResumoFinanceiroClienteDTO>builder()
                    .success(true)
                    .message("Resumo financeiro recuperado com sucesso.")
                    .dados(resumo)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ResumoFinanceiroClienteDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ResumoFinanceiroClienteDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Serviços mais utilizados pelo cliente
     */
    @Operation(summary = "Listar serviços favoritos do cliente")
    @GetMapping("/{id}/servicos-favoritos")
    public ResponseEntity<ResponseAPI<List<ServicoEstatisticaDTO>>> getServicosFavoritos(@PathVariable Long id) {
        try {
            List<ServicoEstatisticaDTO> servicosFavoritos = clienteService.getServicosFavoritosCliente(id);

            return ResponseEntity.ok(ResponseAPI.<List<ServicoEstatisticaDTO>>builder()
                    .success(true)
                    .message("Serviços favoritos recuperados com sucesso.")
                    .dados(servicosFavoritos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ServicoEstatisticaDTO>>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // =============== BUSCA E FILTROS ===============

    /**
     * Buscar clientes por termo de pesquisa
     */
    @Operation(summary = "Buscar clientes por termo")
    @GetMapping("/buscar")
    public ResponseEntity<ResponseAPI<List<ClienteDTO>>> buscarClientes(
            @RequestParam String termo) {

        try {
            if (termo == null || termo.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseAPI.<List<ClienteDTO>>builder()
                                .success(false)
                                .message("Termo de busca é obrigatório.")
                                .errorCode(400)
                                .build());
            }

            List<ClienteDTO> clientes = clienteService.buscarClientes(termo.trim());

            return ResponseEntity.ok(ResponseAPI.<List<ClienteDTO>>builder()
                    .success(true)
                    .message("Busca realizada com sucesso.")
                    .dados(clientes)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ClienteDTO>>builder()
                            .success(false)
                            .message("Erro interno na busca: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // =============== ENDPOINTS ESPECIAIS ===============

    /**
     * Aniversariantes do mês
     */
    @Operation(summary = "Listar aniversariantes do mês")
    @GetMapping("/aniversariantes")
    public ResponseEntity<ResponseAPI<List<ClienteAniversarianteDTO>>> getAniversariantes(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano) {

        try {
            List<ClienteAniversarianteDTO> aniversariantes = clienteService.getAniversariantes(mes, ano);

            return ResponseEntity.ok(ResponseAPI.<List<ClienteAniversarianteDTO>>builder()
                    .success(true)
                    .message("Aniversariantes recuperados com sucesso.")
                    .dados(aniversariantes)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ClienteAniversarianteDTO>>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Top clientes por valor gasto
     */
    @Operation(summary = "Listar top clientes por valor gasto")
    @GetMapping("/top-clientes")
    public ResponseEntity<ResponseAPI<List<TopClienteDTO>>> getTopClientes(
            @RequestParam(defaultValue = "10") int limite) {

        try {
            List<TopClienteDTO> topClientes = clienteService.getTopClientes(limite);

            return ResponseEntity.ok(ResponseAPI.<List<TopClienteDTO>>builder()
                    .success(true)
                    .message("Top clientes recuperados com sucesso.")
                    .dados(topClientes)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<TopClienteDTO>>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Alternar status de ativação do cliente (toggle)
     */
    @Operation(summary = "Ativar/desativar cliente (toggle)")
    @PutMapping("/{id}/status")
    public ResponseEntity<ResponseAPI<ClienteDTO>> toggleStatusCliente(@PathVariable Long id) {

        try {
            ClienteDTO clienteAtualizado = clienteService.toggleStatusCliente(id);

            String statusMsg = clienteAtualizado.isAtivo() ? "ativado" : "desativado";
            return ResponseEntity.ok(ResponseAPI.<ClienteDTO>builder()
                    .success(true)
                    .message("Cliente " + statusMsg + " com sucesso.")
                    .dados(clienteAtualizado)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ClienteDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ClienteDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Estatísticas gerais dos clientes
     */
    @Operation(summary = "Obter estatísticas gerais de clientes")
    @GetMapping("/estatisticas")
    public ResponseEntity<ResponseAPI<EstatisticasClientesDTO>> getEstatisticasClientes() {
        try {
            EstatisticasClientesDTO estatisticas = clienteService.getEstatisticas();

            return ResponseEntity.ok(ResponseAPI.<EstatisticasClientesDTO>builder()
                    .success(true)
                    .message("Estatísticas recuperadas com sucesso.")
                    .dados(estatisticas)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<EstatisticasClientesDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
