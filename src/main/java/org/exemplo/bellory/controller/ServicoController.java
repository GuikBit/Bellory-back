package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.dto.ServicoAgendamento;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.service.ServicoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<ResponseAPI<Servico>> postServico(@RequestBody Servico servico) {
        try {
            Servico novoServico = servicoService.createServico(servico);
            return ResponseEntity
                    .status(HttpStatus.CREATED) // Status 201 para criação bem-sucedida
                    .body(ResponseAPI.<Servico>builder()
                            .success(true)
                            .message("Serviço criado com sucesso!")
                            .dados(novoServico)
                            .build());
        } catch (IllegalArgumentException e) {
            // Captura exceções de validação do serviço
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST) // Status 400 para erros de requisição
                    .body(ResponseAPI.<Servico>builder()
                            .success(false)
                            .message("Erro ao criar serviço: " + e.getMessage())
                            .errorCode(400) // Opcional: Código de erro customizado
                            .build());
        } catch (Exception e) {
            // Captura outras exceções inesperadas
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR) // Status 500 para erros internos
                    .body(ResponseAPI.<Servico>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao criar o serviço.")
                            .errorCode(500)
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<Servico>> updateServico(@PathVariable Long id, @RequestBody Servico servico) {
        try {
            Servico servicoAtualizado = servicoService.updateServico(id, servico);
            return ResponseEntity
                    .status(HttpStatus.OK) // Status 200 OK para atualização bem-sucedida
                    .body(ResponseAPI.<Servico>builder()
                            .success(true)
                            .message("Serviço atualizado com sucesso!")
                            .dados(servicoAtualizado)
                            .build());
        } catch (IllegalArgumentException e) {
            // Captura exceções de validação (serviço não encontrado, nome duplicado, dados inválidos)
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST) // Ou HttpStatus.NOT_FOUND se for específico de não encontrado
                    .body(ResponseAPI.<Servico>builder()
                            .success(false)
                            .message("Erro ao atualizar serviço: " + e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Servico>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao atualizar o serviço.")
                            .errorCode(500)
                            .build());
        }
    }
}
