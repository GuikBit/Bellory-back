package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.service.CargoService;
import org.exemplo.bellory.service.FileStorageService;
import org.exemplo.bellory.service.FuncionarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/funcionario")
@Tag(name = "Funcionários", description = "Gerenciamento de funcionários, cargos e fotos de perfil")
public class FuncionarioController {

    FileStorageService fileStorageService;
    FuncionarioService funcionarioService;
    CargoService cargoService;

    public FuncionarioController(FuncionarioService funcionarioService, CargoService cargoService, FileStorageService fileStorageService) {
        this.funcionarioService = funcionarioService;
        this.cargoService = cargoService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    @Operation(summary = "Listar todos os funcionários")
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
    @Operation(summary = "Buscar funcionário por ID")
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
    @Operation(summary = "Listar funcionários para agendamento")
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
    @Operation(summary = "Criar novo funcionário")
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
    @Operation(summary = "Atualizar funcionário")
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
    @Operation(summary = "Desativar funcionário")
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

    @GetMapping("/validar-username")
    @Operation(summary = "Validar disponibilidade de username")
    public ResponseEntity<ResponseAPI<Boolean>> validarUsername(@RequestParam String username) {
        try {
            boolean existe = funcionarioService.existeUsername(username);

            String mensagem = existe ?
                    "Username '" + username + "' já está em uso." :
                    "Username '" + username + "' está disponível.";

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<Boolean>builder()
                            .success(true)
                            .message(mensagem)
                            .dados(existe)
                            .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Boolean>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Boolean>builder()
                            .success(false)
                            .message("Erro interno ao validar username: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PostMapping("/cargo")
    @Operation(summary = "Criar novo cargo")
    public ResponseEntity<ResponseAPI<CargoDTO>> createCargo(@RequestBody CargoDTO cargoDTO) {
        try {
            CargoDTO criado = cargoService.createCargo(cargoDTO);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ResponseAPI.<CargoDTO>builder()
                            .success(true)
                            .message("Cargo criado com sucesso.")
                            .dados(criado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<CargoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<CargoDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao criar o cargo.")
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/cargo")
    @Operation(summary = "Listar todos os cargos")
    public ResponseEntity<ResponseAPI<List<CargoDTO>>> getCargos() {
        try {
            List<CargoDTO> lista = cargoService.getAllCargos();

            if (lista.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.NO_CONTENT)
                        .body(ResponseAPI.<List<CargoDTO>>builder()
                                .success(true)
                                .message("Nenhum cargo encontrado.")
                                .dados(lista)
                                .build());
            }

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<List<CargoDTO>>builder()
                            .success(true)
                            .message("Lista de cargos recuperada com sucesso.")
                            .dados(lista)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<CargoDTO>>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao listar os cargos.")
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/cargo/{id}")
    @Operation(summary = "Buscar cargo por ID")
    public ResponseEntity<ResponseAPI<CargoDTO>> getCargoById(@PathVariable Long id) {
        try {
            CargoDTO dto = cargoService.getCargoById(id);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<CargoDTO>builder()
                            .success(true)
                            .message("Cargo encontrado com sucesso.")
                            .dados(dto)
                            .build());
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<CargoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<CargoDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar o cargo.")
                            .errorCode(500)
                            .build());
        }
    }

    @PutMapping("/cargo/{id}")
    @Operation(summary = "Atualizar cargo")
    public ResponseEntity<ResponseAPI<CargoDTO>> updateCargo(@PathVariable Long id, @RequestBody CargoDTO cargoDTO) {
        try {
            CargoDTO atualizado = cargoService.updateCargo(id, cargoDTO);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<CargoDTO>builder()
                            .success(true)
                            .message("Cargo atualizado com sucesso.")
                            .dados(atualizado)
                            .build());
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<CargoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<CargoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<CargoDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao atualizar o cargo.")
                            .errorCode(500)
                            .build());
        }
    }

    @DeleteMapping("/cargo/{id}")
    @Operation(summary = "Desativar cargo")
    public ResponseEntity<ResponseAPI<Void>> deleteCargo(@PathVariable Long id) {
        try {
            cargoService.deleteCargo(id);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<Void>builder()
                            .success(true)
                            .message("Cargo desativado com sucesso.")
                            .build());
        } catch (NoSuchElementException e) {
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
                            .message("Ocorreu um erro interno ao desativar o cargo.")
                            .errorCode(500)
                            .build());
        }
    }

    @PostMapping("/{id}/foto-perfil")
    @Operation(summary = "Upload de foto de perfil")
    public ResponseEntity<ResponseAPI<Map<String, String>>> uploadFotoPerfil(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            Map<String, String> resultado = funcionarioService.uploadFotoPerfil(id, file);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(true)
                            .message("Foto de perfil atualizada com sucesso.")
                            .dados(resultado) // Contém: { filename, url, relativePath }
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message("Erro ao fazer upload da foto de perfil.")
                            .errorCode(500)
                            .build());
        }
    }

    // OPÇÃO 1: Redirecionar para URL da imagem (RECOMENDADO)
//    @GetMapping("/{id}/foto-perfil")
//    public ResponseEntity<?> downloadFotoPerfil(@PathVariable Long id) {
//        try {
//            Map<String, Object> resultado = funcionarioService.downloadFotoPerfil(id);
//            String imageUrl = (String) resultado.get("url");
//
//            // Redirecionar para a URL da imagem servida pelo Nginx
//            return ResponseEntity
//                    .status(HttpStatus.FOUND)
//                    .location(URI.create(imageUrl))
//                    .build();
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        } catch (SecurityException e) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }

// OPÇÃO 2: Retornar JSON com URL (alternativa)

    @GetMapping("/{id}/foto-perfil")
    @Operation(summary = "Obter foto de perfil")
    public ResponseEntity<ResponseAPI<Map<String, String>>> downloadFotoPerfil(@PathVariable Long id) {
        try {
            Map<String, Object> resultado = funcionarioService.downloadFotoPerfil(id);
            String imageUrl = (String) resultado.get("url");
            String filename = (String) resultado.get("filename");

            Map<String, String> response = new HashMap<>();
            response.put("url", imageUrl);
            response.put("filename", filename);

            return ResponseEntity.ok(ResponseAPI.<Map<String, String>>builder()
                    .success(true)
                    .dados(response)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message("Acesso negado")
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, String>>builder()
                            .success(false)
                            .message("Erro ao buscar foto")
                            .errorCode(500)
                            .build());
        }
    }


    @DeleteMapping("/{id}/foto-perfil")
    @Operation(summary = "Remover foto de perfil")
    public ResponseEntity<ResponseAPI<Void>> deleteFotoPerfil(@PathVariable Long id) {
        try {
            funcionarioService.deleteFotoPerfil(id);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<Void>builder()
                            .success(true)
                            .message("Foto de perfil removida com sucesso.")
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao remover foto de perfil.")
                            .errorCode(500)
                            .build());
        }
    }

}
