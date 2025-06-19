package org.exemplo.bellory.controller;


import org.exemplo.bellory.model.dto.DisponibilidadeRequest;
import org.exemplo.bellory.model.dto.HorarioDisponivelResponse;
import org.exemplo.bellory.service.AgendamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agendamento")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    // Endpoint para buscar a disponibilidade
    @PostMapping("/disponibilidade")
    public ResponseEntity<List<HorarioDisponivelResponse>> getDisponibilidade(@RequestBody DisponibilidadeRequest request) {
        if (request.getFuncionarioId() == null || request.getDataDesejada() == null || request.getServicoIds() == null || request.getServicoIds().isEmpty()) {
            return ResponseEntity.badRequest().build(); // Ou lance uma exceção mais específica
        }
        List<HorarioDisponivelResponse> horarios = agendamentoService.getHorariosDisponiveis(request);
        return ResponseEntity.ok(horarios);
    }

    // ... (Seu endpoint existente para criar agendamento)
    // @PostMapping
    // public ResponseEntity<Agendamento> criarAgendamento(@RequestBody Agendamento agendamento) {
    //    Agendamento novo = agendamentoService.criarAgendamento(agendamento);
    //    return ResponseEntity.status(HttpStatus.CREATED).body(novo);
    // }
}
