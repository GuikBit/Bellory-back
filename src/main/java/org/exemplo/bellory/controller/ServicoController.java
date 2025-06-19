package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.dto.ServicoAgendamento;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.service.ServicoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servico")
public class ServicoController {

    ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @GetMapping
    public ResponseEntity<ResponseAPI<List<Servico>>> getServicoList() {
        List<Servico> servicos = servicoService.getListAllServicos();

        if (servicos.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT) // Ou HttpStatus.NO_CONTENT, dependendo da sua regra
                    .body(ResponseAPI.<List<Servico>>builder()
                    .success(true)
                    .message("Nenhum serviço encontrado.")
                    .dados(servicos) // Ainda envia a lista vazia
                    .build());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseAPI.<List<Servico>>builder()
                .success(true)
                .message("Lista de serviços recuperada com sucesso.")
                .dados(servicos)
                .build());
    }

    @GetMapping("/agendamento")
    public ResponseEntity<ResponseAPI<List<ServicoAgendamento>>> getServicoAgendamentoList() {
        List<ServicoAgendamento> servicos = servicoService.getListAgendamentoServicos();

        if (servicos.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT) // Ou HttpStatus.NO_CONTENT, dependendo da sua regra
                    .body(ResponseAPI.<List<ServicoAgendamento>>builder()
                            .success(true)
                            .message("Nenhum serviço encontrado.")
                            .dados(servicos) // Ainda envia a lista vazia
                            .build());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseAPI.<List<ServicoAgendamento>>builder()
                        .success(true)
                        .message("Lista de serviços recuperada com sucesso.")
                        .dados(servicos)
                        .build());
    }
}
