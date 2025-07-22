package org.exemplo.bellory.service;

import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> getListAllCliente() {
        return this.clienteRepository.findAll();
    }


}
