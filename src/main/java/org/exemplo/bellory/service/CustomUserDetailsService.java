package org.exemplo.bellory.service;

import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    // Injetar os repositórios das entidades concretas
    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;

    public CustomUserDetailsService(FuncionarioRepository funcionarioRepository, ClienteRepository clienteRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Tenta encontrar como Funcionário primeiro
        return funcionarioRepository.findByUsername(username)
                .map(UserDetails.class::cast)
                // Se não encontrar, tenta como Cliente
                .orElseGet(() -> clienteRepository.findByUsername(username)
                        .map(UserDetails.class::cast)
                        .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username)));
    }
}
