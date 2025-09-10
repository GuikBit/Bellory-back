package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.dto.*;
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
        //this.cargoService = cargoService;
    }

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

    // NOVO ENDPOINT: Buscar funcionário por ID com todos os dados
    @GetMapping("/{id}")
    public ResponseEntity<ResponseAPI<FuncionarioDTO>> getFuncionarioById(@PathVariable Long id) {
        try {
            FuncionarioDTO funcionarioDTO = funcionarioService.getFuncionarioById(id);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<FuncionarioDTO>builder()
                            .success(true)
                            .message("Funcionário encontrado com sucesso.")
                            .dados(funcionarioDTO)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<FuncionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<FuncionarioDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar o funcionário.")
                            .errorCode(500)
                            .build());
        }
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
    public ResponseEntity<ResponseAPI<Funcionario>> postFuncionario(@RequestBody FuncionarioCreateDTO funcionarioDTO) {
        try {
            Funcionario novoFuncionario = funcionarioService.postNewFuncionario(funcionarioDTO);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ResponseAPI.<Funcionario>builder()
                            .success(true)
                            .message("Funcionário criado com sucesso.")
                            .dados(novoFuncionario)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Funcionario>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Funcionario>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao criar o funcionário: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<Funcionario>> updateFuncionario(@PathVariable Long id, @RequestBody FuncionarioUpdateDTO funcionarioDTO) {
        try {
            Funcionario funcionarioAtualizado = funcionarioService.updateFuncionario(id, funcionarioDTO);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<Funcionario>builder()
                            .success(true)
                            .message("Funcionário atualizado com sucesso.")
                            .dados(funcionarioAtualizado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Funcionario>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Funcionario>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao atualizar o funcionário.")
                            .errorCode(500)
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> deleteFuncionario(@PathVariable Long id) {
        try {
            funcionarioService.deleteFuncionario(id);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<Void>builder()
                            .success(true)
                            .message("Funcionário desativado com sucesso.")
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao desativar o funcionário.")
                            .errorCode(500)
                            .build());
        }
    }

//    @PostMapping("/cargo")
//    public ResponseEntity<ResponseAPI<Cargo>> createCargo(@RequestBody CargoDTO cargoDTO){
//        try {
//            CargoDTO cargo = cargoService.createCargo(cargoDTO);
//            return ResponseEntity
//                    .status(HttpStatus.OK)
//                    .body(ResponseAPI.<Void>builder()
//                            .success(true)
//                            .message("Funcionário desativado com sucesso.")
//                            .build());
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity
//                    .status(HttpStatus.NOT_FOUND)
//                    .body(ResponseAPI.<Void>builder()
//                            .success(false)
//                            .message(e.getMessage())
//                            .errorCode(404)
//                            .build());
//        } catch (Exception e) {
//            return ResponseEntity
//                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ResponseAPI.<Void>builder()
//                            .success(false)
//                            .message("Ocorreu um erro interno ao desativar o funcionário.")
//                            .errorCode(500)
//                            .build());
//        }
//    }


}
