package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.dto.FuncionarioAgendamento;
import org.exemplo.bellory.model.dto.FuncionarioDTO; // Importar o DTO
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
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

    // O tipo de retorno agora é ResponseAPI<List<FuncionarioDTO>>
    @GetMapping
    public ResponseEntity<ResponseAPI<List<FuncionarioDTO>>> getFuncionarioList() {
        List<FuncionarioDTO> funcionariosDTO = funcionarioService.getListAllFuncionarios();

        if (funcionariosDTO.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body(ResponseAPI.<List<FuncionarioDTO>>builder()
                            .success(true)
                            .message("Nenhum funcionário encontrado.")
                            .dados(funcionariosDTO)
                            .build());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseAPI.<List<FuncionarioDTO>>builder()
                        .success(true)
                        .message("Lista de funcionários recuperada com sucesso.")
                        .dados(funcionariosDTO)
                        .build());
    }

    @GetMapping("/agendamento")
    public ResponseEntity<ResponseAPI<List<FuncionarioAgendamento>>> getFuncionarioListAgendamento() {
        List<FuncionarioAgendamento> funcionarios = funcionarioService.getListAllFuncionariosAgendamento();

        if (funcionarios.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body(ResponseAPI.<List<FuncionarioAgendamento>>builder()
                            .success(true)
                            .message("Nenhum funcionário encontrado.")
                            .dados(funcionarios)
                            .build());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseAPI.<List<FuncionarioAgendamento>>builder()
                        .success(true)
                        .message("Lista de funcionários para agendamento recuperada com sucesso.")
                        .dados(funcionarios)
                        .build());
    }

    @PostMapping
    public ResponseEntity<ResponseAPI<Funcionario>> postFuncionario(@RequestBody Funcionario funcionario) {
        Funcionario func = funcionarioService.postNewFuncionario(funcionario);

        if (func.getId() == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Funcionario>builder()
                            .success(false)
                            .message("Erro ao criar funcionário.")
                            .dados(null)
                            .build());
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseAPI.<Funcionario>builder()
                        .success(true)
                        .message("Funcionário criado com sucesso.")
                        .dados(func)
                        .build());
    }
}
