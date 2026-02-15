package org.exemplo.bellory.service;

import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.users.Admin;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.users.AdminRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;
    private final AdminRepository adminRepository;

    public CustomUserDetailsService(FuncionarioRepository funcionarioRepository, ClienteRepository clienteRepository, AdminRepository adminRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.clienteRepository = clienteRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        // 1. Tentar buscar como ADMIN primeiro (maior prioridade)
        Optional<Admin> adminOpt = organizacaoId != null
                ? adminRepository.findByUsernameAndOrganizacao_Id(username, organizacaoId)
                : adminRepository.findByUsername(username);
        if (adminOpt.isPresent()) {
            return adminOpt.get();
        }

        // 2. Tentar buscar como FUNCIONÁRIO
        Optional<Funcionario> funcionarioOpt = organizacaoId != null
                ? funcionarioRepository.findByUsernameAndOrganizacao_Id(username, organizacaoId)
                : funcionarioRepository.findByUsername(username);
        if (funcionarioOpt.isPresent()) {
            return funcionarioOpt.get();
        }

        // 3. Tentar buscar como CLIENTE
        Optional<Cliente> clienteOpt = organizacaoId != null
                ? clienteRepository.findByUsernameAndOrganizacao_Id(username, organizacaoId)
                : clienteRepository.findByUsername(username);
        if (clienteOpt.isPresent()) {
            return clienteOpt.get();
        }

        throw new UsernameNotFoundException("Usuário não encontrado: " + username);
    }
}
