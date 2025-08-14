package org.exemplo.bellory.controller;


import org.exemplo.bellory.model.dto.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.AgendamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agendamento")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @PostMapping("/disponibilidade")
    public ResponseEntity<List<HorarioDisponivelResponse>> getDisponibilidade(@RequestBody DisponibilidadeRequest request) {
        if (request.getFuncionarioId() == null || request.getDataDesejada() == null || request.getServicoIds() == null || request.getServicoIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<HorarioDisponivelResponse> horarios = agendamentoService.getHorariosDisponiveis(request);
        return ResponseEntity.ok(horarios);
    }


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

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> cancelAgendamento(@PathVariable Long id) {
        try {
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao cancelar o agendamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // Endpoint para alterar status do agendamento
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
                    .message("Agenda do funcionário para " + dataConsulta.toString() + " recuperada com sucesso.")
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

}
