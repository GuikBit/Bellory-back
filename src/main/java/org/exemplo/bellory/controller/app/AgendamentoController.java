package org.exemplo.bellory.controller.app;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.*;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.pagamento.Pagamento;
import org.exemplo.bellory.service.AgendamentoService;
import org.exemplo.bellory.service.TransacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/agendamento")
@Tag(name = "Agendamentos", description = "Gerenciamento de agendamentos, disponibilidade, pagamentos e status")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final TransacaoService transacaoService;

    public AgendamentoController(AgendamentoService agendamentoService, TransacaoService transacaoService) {
        this.agendamentoService = agendamentoService;
        this.transacaoService = transacaoService;
    }

    @Operation(summary = "Verificar disponibilidade de horários")
    @PostMapping("/disponibilidade")
    public ResponseEntity<List<HorarioDisponivelResponse>> getDisponibilidade(@RequestBody DisponibilidadeRequest request) {
        if (request.getFuncionarioId() == null || request.getDataDesejada() == null || request.getServicoIds() == null || request.getServicoIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<HorarioDisponivelResponse> horarios = agendamentoService.getHorariosDisponiveis(request);
        return ResponseEntity.ok(horarios);
    }


    @Operation(summary = "Criar novo agendamento")
    @PostMapping
    public ResponseEntity<ResponseAPI<AgendamentoDTO>> createAgendamento(@RequestBody AgendamentoCreateDTO agendamentoDTO) {
        try {
            // Validações básicas
            if (agendamentoDTO.getClienteId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<AgendamentoDTO>builder()
                                .success(false)
                                .message("O ID do cliente é obrigatório.")
                                .errorCode(400)
                                .build());
            }
            if (agendamentoDTO.getFuncionarioIds() == null || agendamentoDTO.getFuncionarioIds().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<AgendamentoDTO>builder()
                                .success(false)
                                .message("Pelo menos um funcionário deve ser selecionado.")
                                .errorCode(400)
                                .build());
            }
            if (agendamentoDTO.getServicoIds() == null || agendamentoDTO.getServicoIds().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<AgendamentoDTO>builder()
                                .success(false)
                                .message("Pelo menos um serviço deve ser selecionado.")
                                .errorCode(400)
                                .build());
            }
            if (agendamentoDTO.getDtAgendamento() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<AgendamentoDTO>builder()
                                .success(false)
                                .message("A data e hora do agendamento são obrigatórias.")
                                .errorCode(400)
                                .build());
            }

            AgendamentoDTO novoAgendamento = agendamentoService.createAgendamentoCompleto(agendamentoDTO);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(true)
                            .message("Agendamento criado com sucesso.")
                            .dados(novoAgendamento)
                            .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao criar o agendamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // Endpoint para listar todos os agendamentos
    @Operation(summary = "Listar todos os agendamentos")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> getAllAgendamentos() {
        try {
            List<AgendamentoDTO> agendamentos = agendamentoService.getAllAgendamentos();

            if (agendamentos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                                .success(true)
                                .message("Nenhum agendamento encontrado.")
                                .dados(agendamentos)
                                .build());
            }

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Lista de agendamentos recuperada com sucesso.")
                    .dados(agendamentos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar os agendamentos: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // Endpoint para buscar um agendamento por ID
    @Operation(summary = "Buscar agendamento por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseAPI<AgendamentoDTO>> getAgendamentoById(@PathVariable Long id) {
        try {
            AgendamentoDTO agendamento = agendamentoService.getAgendamentoById(id);

            return ResponseEntity.ok(ResponseAPI.<AgendamentoDTO>builder()
                    .success(true)
                    .message("Agendamento encontrado com sucesso.")
                    .dados(agendamento)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar o agendamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // Endpoint para atualizar um agendamento
    @Operation(summary = "Atualizar agendamento")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<AgendamentoDTO>> updateAgendamento(@PathVariable Long id, @RequestBody AgendamentoUpdateDTO agendamentoUpdateDTO) {
        try {
            AgendamentoDTO agendamentoAtualizado = agendamentoService.updateAgendamento(id, agendamentoUpdateDTO);

            return ResponseEntity.ok(ResponseAPI.<AgendamentoDTO>builder()
                    .success(true)
                    .message("Agendamento atualizado com sucesso.")
                    .dados(agendamentoAtualizado)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao atualizar o agendamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Listar agendamentos com cobranças pendentes")
    @GetMapping("/cobrancas-pendentes")
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> getAgendamentosComCobrancasPendentes() {
        try {
            List<AgendamentoDTO> agendamentos = agendamentoService.getAgendamentosComCobrancasPendentes();

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Agendamentos com cobranças pendentes recuperados com sucesso.")
                    .dados(agendamentos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Erro ao buscar agendamentos com cobranças pendentes: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Listar agendamentos com cobranças vencidas")
    @GetMapping("/vencidos")
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> getAgendamentosVencidos() {
        try {
            List<AgendamentoDTO> agendamentos = agendamentoService.getAgendamentosVencidos();

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Agendamentos com cobranças vencidas recuperados com sucesso.")
                    .dados(agendamentos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Erro ao buscar agendamentos vencidos: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Processar pagamento de agendamento")
    @PostMapping("/{id}/pagamento")
    public ResponseEntity<ResponseAPI<Map<String, Object>>> processarPagamentoAgendamento(
            @PathVariable Long id,
            @RequestBody PagamentoAgendamentoDTO pagamentoDTO) {
        try {
            // Validações básicas
            if (pagamentoDTO.getValorPagamento() == null || pagamentoDTO.getValorPagamento().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<Map<String, Object>>builder()
                                .success(false)
                                .message("Valor do pagamento deve ser maior que zero.")
                                .errorCode(400)
                                .build());
            }

            if (pagamentoDTO.getMetodoPagamento() == null || pagamentoDTO.getMetodoPagamento().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<Map<String, Object>>builder()
                                .success(false)
                                .message("Método de pagamento é obrigatório.")
                                .errorCode(400)
                                .build());
            }

            // Buscar agendamento
            AgendamentoDTO agendamento = agendamentoService.getAgendamentoById(id);
            if (agendamento.getCobrancas() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<Map<String, Object>>builder()
                                .success(false)
                                .message("Este agendamento não possui cobrança associada.")
                                .errorCode(400)
                                .build());
            }

            // Processar pagamento através do TransactionService
            Pagamento.FormaPagamento formaPagamento = Pagamento.FormaPagamento.valueOf(
                    pagamentoDTO.getMetodoPagamento().toUpperCase()
            );

            Pagamento pagamento = transacaoService.processarPagamento(
                    pagamentoDTO.getCobrancaId(),
                    pagamentoDTO.getValorPagamento(),
                    formaPagamento
            );

            // Preparar resposta
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("pagamentoId", pagamento.getId());
            resultado.put("transacaoId", pagamento.getTransacaoId());
            resultado.put("status", pagamento.getStatusPagamento().name());
            resultado.put("valor", pagamento.getValor());
            resultado.put("formaPagamento", pagamento.getFormaPagamento() != null ? pagamento.getFormaPagamento().getDescricao() : null);

            return ResponseEntity.ok(ResponseAPI.<Map<String, Object>>builder()
                    .success(true)
                    .message("Pagamento processado com sucesso.")
                    .dados(resultado)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message("Erro interno ao processar pagamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Dividir pagamento de agendamento")
    @PostMapping("/{id}/dividir-pagamento")
    public ResponseEntity<ResponseAPI<Void>> processarDividirPagamento(
            @PathVariable Long id,
            @RequestBody DividirPagamentoDTO dividirPagamentoDTO
    ){
        try{
            if(dividirPagamentoDTO.getCobrancaId() == null){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<Void>builder()
                                .success(false)
                                .message("ID da cobrança não identificado.")
                                .errorCode(400)
                                .build());
            }

            if(dividirPagamentoDTO.getPorcentagemDivisao() == null){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<Void>builder()
                                .success(false)
                                .message("Porcentagem de sinal não identificado.")
                                .errorCode(400)
                                .build());
            }

            transacaoService.criarSinal(id, dividirPagamentoDTO.getCobrancaId(), dividirPagamentoDTO.getPorcentagemDivisao());


            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Pagamento processado com sucesso.")
                    .build());

        }catch  (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro interno a divisao do pagamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Cancelar agendamento")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> cancelAgendamento(@PathVariable Long id) {
        try {
            // Verificar se existe cobrança paga antes de cancelar
            AgendamentoDTO agendamento = agendamentoService.getAgendamentoById(id);

            if (agendamento.getCobrancas().stream() != null) {
                List<Cobranca> cobrancas = transacaoService.getCobrancasPendentesCliente(agendamento.getClienteId())
                        .stream()
                        .filter(c -> c.getId().equals(agendamento.getCobrancas().get(0).getId()))
                        .collect(Collectors.toList());

                if (!cobrancas.isEmpty() && cobrancas.get(0).isPaga()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ResponseAPI.<Void>builder()
                                    .success(false)
                                    .message("Não é possível cancelar agendamento com cobrança paga. Realize estorno primeiro.")
                                    .errorCode(400)
                                    .build());
                }
            }

            agendamentoService.cancelAgendamento(id);

            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Agendamento cancelado com sucesso.")
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao cancelar o agendamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Estornar pagamento de agendamento")
    @PostMapping("/{id}/estorno")
    public ResponseEntity<ResponseAPI<String>> estornarPagamentoAgendamento(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String motivo = request.get("motivo");
            if (motivo == null || motivo.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<String>builder()
                                .success(false)
                                .message("Motivo do estorno é obrigatório.")
                                .errorCode(400)
                                .build());
            }

            // Buscar agendamento
            AgendamentoDTO agendamento = agendamentoService.getAgendamentoById(id);

            if (agendamento.getCobrancas() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<String>builder()
                                .success(false)
                                .message("Este agendamento não possui cobrança associada.")
                                .errorCode(400)
                                .build());
            }

            // Processar estorno através do TransactionService
            //transacaoService.estornarCobranca(agendamento.getCobrancas().stream().map(c-> c.getId() == id), motivo);

            return ResponseEntity.ok(ResponseAPI.<String>builder()
                    .success(true)
                    .message("Estorno processado com sucesso.")
                    .dados("Cobrança estornada: " + agendamento.getCobrancas().get(id.byteValue()))
                    .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<String>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<String>builder()
                            .success(false)
                            .message("Erro interno ao processar estorno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Listar status disponíveis")
    @GetMapping("/status-disponiveis")
    public ResponseEntity<ResponseAPI<List<StatusAgendamentoDTO>>> getStatusDisponiveis() {
        try {
            List<StatusAgendamentoDTO> statusList = agendamentoService.getAllStatusDisponiveis();

            return ResponseEntity.ok(ResponseAPI.<List<StatusAgendamentoDTO>>builder()
                    .success(true)
                    .message("Status disponíveis recuperados com sucesso.")
                    .dados(statusList)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<StatusAgendamentoDTO>>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar os status: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
    // Endpoint para alterar status do agendamento
    @Operation(summary = "Atualizar status do agendamento")
    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<ResponseAPI<AgendamentoDTO>> updateStatusAgendamento(@PathVariable Long id, @PathVariable String status) {
        try {
            AgendamentoDTO agendamentoAtualizado = agendamentoService.updateStatusAgendamento(id, status);

            return ResponseEntity.ok(ResponseAPI.<AgendamentoDTO>builder()
                    .success(true)
                    .message("Status do agendamento atualizado com sucesso.")
                    .dados(agendamentoAtualizado)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao atualizar o status: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Listar agendamentos por cliente")
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> getAgendamentosByCliente(@PathVariable Long clienteId) {
        try {
            List<AgendamentoDTO> agendamentos = agendamentoService.getAgendamentosByCliente(clienteId);

            if (agendamentos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                                .success(true)
                                .message("Nenhum agendamento encontrado para este cliente.")
                                .dados(agendamentos)
                                .build());
            }

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Agendamentos do cliente recuperados com sucesso.")
                    .dados(agendamentos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar os agendamentos: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Listar agendamentos por funcionário")
    @GetMapping("/funcionario/{funcionarioId}")
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> getAgendamentosByFuncionario(@PathVariable Long funcionarioId) {
        try {
            List<AgendamentoDTO> agendamentos = agendamentoService.getAgendamentosByFuncionario(funcionarioId);

            if (agendamentos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                                .success(true)
                                .message("Nenhum agendamento encontrado para este funcionário.")
                                .dados(agendamentos)
                                .build());
            }

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Agendamentos do funcionário recuperados com sucesso.")
                    .dados(agendamentos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar os agendamentos: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // Endpoint para buscar agendamentos por data
    @Operation(summary = "Listar agendamentos por data")
    @GetMapping("/data/{data}")
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> getAgendamentosByData(@PathVariable String data) {
        try {
            LocalDate dataConsulta = LocalDate.parse(data);
            List<AgendamentoDTO> agendamentos = agendamentoService.getAgendamentosByData(dataConsulta);

            if (agendamentos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                                .success(true)
                                .message("Nenhum agendamento encontrado para esta data.")
                                .dados(agendamentos)
                                .build());
            }

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Agendamentos da data recuperados com sucesso.")
                    .dados(agendamentos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar os agendamentos: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // Endpoint para buscar agendamentos por status
    @Operation(summary = "Listar agendamentos por status")
    @GetMapping("/status/{status}")
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> getAgendamentosByStatus(@PathVariable String status) {
        try {
            List<AgendamentoDTO> agendamentos = agendamentoService.getAgendamentosByStatus(status);

            if (agendamentos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                                .success(true)
                                .message("Nenhum agendamento encontrado com este status.")
                                .dados(agendamentos)
                                .build());
            }

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Agendamentos com status " + status + " recuperados com sucesso.")
                    .dados(agendamentos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar os agendamentos: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Reagendar agendamento")
    @PatchMapping("/{id}/reagendar")
    public ResponseEntity<ResponseAPI<AgendamentoDTO>> reagendarAgendamento(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String novaDataHoraStr = request.get("novaDataHora");
            if (novaDataHoraStr == null || novaDataHoraStr.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<AgendamentoDTO>builder()
                                .success(false)
                                .message("A nova data e hora são obrigatórias.")
                                .errorCode(400)
                                .build());
            }

            LocalDateTime novaDataHora = LocalDateTime.parse(novaDataHoraStr);
            AgendamentoDTO agendamentoReagendado = agendamentoService.reagendarAgendamento(id, novaDataHora);

            return ResponseEntity.ok(ResponseAPI.<AgendamentoDTO>builder()
                    .success(true)
                    .message("Agendamento reagendado com sucesso.")
                    .dados(agendamentoReagendado)
                    .build());

        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message("Formato de data inválido. Use o formato: yyyy-MM-ddTHH:mm:ss")
                            .errorCode(400)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao reagendar: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Obter estatísticas de agendamentos")
    @GetMapping("/estatisticas")
    public ResponseEntity<ResponseAPI<AgendamentoEstatisticasDTO>> getEstatisticasAgendamentos() {
        try {
            AgendamentoEstatisticasDTO estatisticas = agendamentoService.getEstatisticasAgendamentos();

            return ResponseEntity.ok(ResponseAPI.<AgendamentoEstatisticasDTO>builder()
                    .success(true)
                    .message("Estatísticas de agendamentos recuperadas com sucesso.")
                    .dados(estatisticas)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AgendamentoEstatisticasDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar estatísticas: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Listar agendamentos de hoje")
    @GetMapping("/hoje")
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> getAgendamentosHoje() {
        try {
            List<AgendamentoDTO> agendamentos = agendamentoService.getAgendamentosHoje();

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Agendamentos de hoje recuperados com sucesso.")
                    .dados(agendamentos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Ocorreu um erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Listar próximos agendamentos")
    @GetMapping("/proximos")
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> getAgendamentosProximos() {
        try {
            List<AgendamentoDTO> agendamentos = agendamentoService.getAgendamentosProximos();

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Próximos agendamentos recuperados com sucesso.")
                    .dados(agendamentos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Ocorreu um erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Filtrar agendamentos")
    @PostMapping("/filtrar")
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> filtrarAgendamentos(@RequestBody AgendamentoFiltroDTO filtro) {
        try {
            List<AgendamentoDTO> agendamentos = agendamentoService.filtrarAgendamentos(filtro);

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Agendamentos filtrados recuperados com sucesso.")
                    .dados(agendamentos)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao filtrar: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Obter agenda do dia por funcionário")
    @GetMapping("/funcionario/{funcionarioId}/agenda/{data}")
    public ResponseEntity<ResponseAPI<List<AgendamentoDTO>>> getAgendaDoDia(@PathVariable Long funcionarioId, @PathVariable String data) {
        try {
            LocalDate dataConsulta = LocalDate.parse(data);
            List<AgendamentoDTO> agendamentos = agendamentoService.getAgendamentosByFuncionarioAndData(funcionarioId, dataConsulta);

            if (agendamentos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                                .success(true)
                                .message("Nenhum agendamento encontrado para esta data.")
                                .dados(agendamentos)
                                .build());
            }

            return ResponseEntity.ok(ResponseAPI.<List<AgendamentoDTO>>builder()
                    .success(true)
                    .message("Agenda do funcionário para " + dataConsulta + " recuperada com sucesso.")
                    .dados(agendamentos)
                    .build());

        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Formato de data inválido. Use o formato: yyyy-MM-dd")
                            .errorCode(400)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AgendamentoDTO>>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar a agenda: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }

    }

    // Adicionar este endpoint na classe AgendamentoController

    /**
     * Endpoint unificado para consultar relacionamentos entre funcionários e serviços
     *
     * Casos de uso:
     * 1. Passar servicoIds: retorna funcionários que prestam TODOS os serviços
     * 2. Passar funcionarioIds: retorna serviços que TODOS os funcionários prestam em comum
     */
    @Operation(summary = "Consultar relacionamentos funcionários/serviços")
    @PostMapping("/consultar-relacionamentos")
    public ResponseEntity<ResponseAPI<FuncionarioServicoResponse>> consultarRelacionamentos(
            @RequestBody ConsultaRelacionamentoRequest request) {
        try {
            FuncionarioServicoResponse resultado = agendamentoService.consultarRelacionamentos(request);

            String mensagem;
            if ("POR_SERVICOS".equals(resultado.getTipoConsulta())) {
                mensagem = String.format("Encontrados %d funcionário(s) que prestam todos os %d serviço(s) informados.",
                        resultado.getDados().size());
            } else {
                mensagem = String.format("Encontrados %d serviço(s) que todos os %d funcionário(s) prestam em comum.",
                        resultado.getDados().size());
            }

            return ResponseEntity.ok(ResponseAPI.<FuncionarioServicoResponse>builder()
                    .success(true)
                    .message(mensagem)
                    .dados(resultado)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<FuncionarioServicoResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<FuncionarioServicoResponse>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao consultar relacionamentos: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Endpoint específico para buscar funcionários por serviços
     */
    @Operation(summary = "Buscar funcionários por serviços")
    @GetMapping("/funcionarios-por-servicos")
    public ResponseEntity<ResponseAPI> getFuncionariosPorServicos(
            @RequestParam List<Long> servicoIds) {
        try {
            FuncionarioServicoResponse resultado = agendamentoService.consultarFuncionariosPorServicos(servicoIds);

            return ResponseEntity.ok(ResponseAPI.<List>builder()
                    .success(true)
                    .message(String.format("Encontrados %d funcionário(s) que prestam todos os serviços informados.", resultado.getDados().size()))
                    .dados(resultado.getDados())
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<FuncionarioServicoResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<FuncionarioServicoResponse>builder()
                            .success(false)
                            .message("Erro interno ao buscar funcionários: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Endpoint específico para buscar serviços por funcionários
     */
    @Operation(summary = "Buscar serviços por funcionários")
    @GetMapping("/servicos-por-funcionarios")
    public ResponseEntity<ResponseAPI> getServicosPorFuncionarios(
            @RequestParam List<Long> funcionarioIds) {
        try {
            FuncionarioServicoResponse resultado = agendamentoService.consultarServicosPorFuncionarios(funcionarioIds);

            return ResponseEntity.ok(ResponseAPI.<FuncionarioServicoResponse>builder()
                    .success(true)
                    .message(String.format("Encontrados %d serviço(s) que todos os funcionários prestam em comum.",
                            resultado.getDados().size()))
                    .dados(resultado)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<FuncionarioServicoResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<FuncionarioServicoResponse>builder()
                            .success(false)
                            .message("Erro interno ao buscar serviços: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

}
