package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.dto.FuncionarioAgendamento;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.service.FuncionarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funcionario")
public class FuncionarioController {

    FuncionarioService funcionarioService;

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    public ResponseEntity<ResponseAPI<List<Funcionario>>> getFuncionarioList() {
        List<Funcionario> funcionarios = funcionarioService.getListAllFuncionarios();

        if (funcionarios.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT) // Ou HttpStatus.NO_CONTENT, dependendo da sua regra
                    .body(ResponseAPI.<List<Funcionario>>builder()
                            .success(true)
                            .message("Nenhum serviço encontrado.")
                            .dados(funcionarios) // Ainda envia a lista vazia
                            .build());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseAPI.<List<Funcionario>>builder()
                        .success(true)
                        .message("Lista de serviços recuperada com sucesso.")
                        .dados(funcionarios)
                        .build());
    }

    @GetMapping("/agendamento")
    public ResponseEntity<ResponseAPI<List<FuncionarioAgendamento>>> getFuncionarioListAgendamento() {
        List<FuncionarioAgendamento> funcionarios = funcionarioService.getListAllFuncionariosAgendamento();

        if (funcionarios.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT) // Ou HttpStatus.NO_CONTENT, dependendo da sua regra
                    .body(ResponseAPI.<List<FuncionarioAgendamento>>builder()
                            .success(true)
                            .message("Nenhum serviço encontrado.")
                            .dados(funcionarios) // Ainda envia a lista vazia
                            .build());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseAPI.<List<FuncionarioAgendamento>>builder()
                        .success(true)
                        .message("Lista de serviços recuperada com sucesso.")
                        .dados(funcionarios)
                        .build());
    }

    @PostMapping
    public ResponseEntity<ResponseAPI<Funcionario>> postFuncionario(@RequestBody Funcionario funcionario) {
        Funcionario func = funcionarioService.postNewFuncionario(funcionario);

        if (func.getId() == null) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT) // Ou HttpStatus.NO_CONTENT, dependendo da sua regra
                    .body(ResponseAPI.<Funcionario>builder()
                            .success(true)
                            .message("Nenhum serviço encontrado.")
                            .dados(func) // Ainda envia a lista vazia
                            .build());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseAPI.<Funcionario>builder()
                        .success(true)
                        .message("Lista de serviços recuperada com sucesso.")
                        .dados(func)
                        .build());
    }
}
