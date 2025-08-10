package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.dto.ClienteDTO; // Importar o DTO
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.ClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cliente")
public class ClienteController {

    ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // O tipo de retorno agora é ResponseAPI<List<ClienteDTO>>
    @GetMapping
    public ResponseEntity<ResponseAPI<List<ClienteDTO>>> getClientes(){

        List<ClienteDTO> clientesDTO = clienteService.getListAllCliente();

        if(clientesDTO.isEmpty()){
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ResponseAPI.<List<ClienteDTO>>builder()
                            .success(true)
                            .message("Nenhum cliente encontrado")
                            .dados(clientesDTO)
                            .build());
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseAPI.<List<ClienteDTO>>builder()
                        .success(true)
                        .message("Lista de clientes recuperada com sucesso.")
                        .dados(clientesDTO)
                        .build());
    }
}
