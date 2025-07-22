package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.service.ClienteService;
import org.exemplo.bellory.service.FuncionarioService;
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

    @GetMapping
    public ResponseEntity<ResponseAPI<List<Cliente>>> getClientes(){

        List<Cliente> clientes = clienteService.getListAllCliente();

        if(clientes.isEmpty()){
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ResponseAPI.<List<Cliente>>builder()
                            .success(true)
                            .message("Nenhum cliente encontrado")
                            .dados(clientes)
                            .build());
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseAPI.<List<Cliente>>builder()
                        .success(true)
                        .message("Lista de clientes recuperada com sucesso.")
                        .dados(clientes)
                        .build());
    }

}
