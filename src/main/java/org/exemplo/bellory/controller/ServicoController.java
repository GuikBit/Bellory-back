package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.dto.ServicoAgendamento;
import org.exemplo.bellory.model.dto.ServicoCreateDTO;
import org.exemplo.bellory.model.dto.ServicoDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.service.ServicoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servico")
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @GetMapping
    public ResponseEntity<ResponseAPI<List<ServicoDTO>>> getServicoList() {
        List<Servico> servicos = servicoService.getListAllServicos();
        List<ServicoDTO> servicosDTO = servicos.stream().map(ServicoDTO::new).collect(Collectors.toList());

        return ResponseEntity.ok(ResponseAPI.<List<ServicoDTO>>builder()
                .success(true)
                .message("Lista de serviços recuperada com sucesso.")
                .dados(servicosDTO)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseAPI<ServicoDTO>> getServicoById(@PathVariable Long id) {
        try {
            Servico servico = servicoService.getServicoById(id);
            return ResponseEntity.ok(ResponseAPI.<ServicoDTO>builder()
                    .success(true)
                    .dados(new ServicoDTO(servico))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ServicoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }

    @GetMapping("/agendamento")
    public ResponseEntity<ResponseAPI<List<ServicoAgendamento>>> getServicoAgendamentoList() {
        List<ServicoAgendamento> servicos = servicoService.getListAgendamentoServicos();
        return ResponseEntity.ok(ResponseAPI.<List<ServicoAgendamento>>builder()
                .success(true)
                .message("Lista de serviços para agendamento recuperada com sucesso.")
                .dados(servicos)
                .build());
    }

    @PostMapping
    public ResponseEntity<ResponseAPI<ServicoDTO>> postServico(@RequestBody ServicoCreateDTO servicoDTO) {
        try {
            Servico novoServico = servicoService.createServico(servicoDTO);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ServicoDTO>builder()
                            .success(true)
                            .message("Serviço criado com sucesso!")
                            .dados(new ServicoDTO(novoServico))
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<ServicoDTO>builder()
                            .success(false)
                            .message("Erro ao criar serviço: " + e.getMessage())
                            .errorCode(400)
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<ServicoDTO>> updateServico(@PathVariable Long id, @RequestBody ServicoCreateDTO servicoDTO) {
        try {
            Servico servicoAtualizado = servicoService.updateServico(id, servicoDTO);
            return ResponseEntity.ok(ResponseAPI.<ServicoDTO>builder()
                    .success(true)
                    .message("Serviço atualizado com sucesso!")
                    .dados(new ServicoDTO(servicoAtualizado))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ServicoDTO>builder()
                            .success(false)
                            .message("Erro ao atualizar serviço: " + e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> deleteServico(@PathVariable Long id) {
        try {
            servicoService.deleteServico(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Serviço desativado com sucesso.")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }
}
