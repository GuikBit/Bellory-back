package org.exemplo.bellory.service;

import org.exemplo.bellory.model.dto.ClienteDTO; // Importar o DTO
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors; // Importar

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // O método agora retorna uma lista de DTOs
    public List<ClienteDTO> getListAllCliente() {
        // 1. Busca as entidades do banco
        List<Cliente> clientes = this.clienteRepository.findAll();

        // 2. Mapeia cada entidade para um DTO
        return clientes.stream()
                .map(cliente -> new ClienteDTO(
                        cliente.getId(),
                        cliente.getNomeCompleto(),
                        cliente.getEmail(),
                        cliente.getTelefone(),
                        cliente.getDataNascimento(),
                        cliente.getRole(),
                        cliente.isAtivo()
                ))
                .collect(Collectors.toList());
    }
}
