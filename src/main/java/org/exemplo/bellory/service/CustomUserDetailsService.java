package org.exemplo.bellory.service;

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

    // Injetar os repositórios das entidades concretas
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
        // Normalizar username

        // 1. Tentar buscar como ADMIN primeiro (maior prioridade)
        System.out.println("🔍 Buscando como ADMIN...");
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            System.out.println("✅ ADMIN encontrado!");
            System.out.println("   - ID: " + admin.getId());
            System.out.println("   - Nome: " + admin.getNomeCompleto());
            System.out.println("   - Email: " + admin.getEmail());
            System.out.println("   - Role: " + admin.getRole());
            System.out.println("   - Ativo: " + admin.isEnabled());
            System.out.println("   - Senha começa com: " + (admin.getPassword() != null ? admin.getPassword().substring(0, Math.min(10, admin.getPassword().length())) : "NULL"));
            return admin;
        }
        System.out.println("❌ ADMIN não encontrado");

        // 2. Tentar buscar como FUNCIONÁRIO
        System.out.println("🔍 Buscando como FUNCIONÁRIO...");
        Optional<Funcionario> funcionarioOpt = funcionarioRepository.findByUsername(username);
        if (funcionarioOpt.isPresent()) {
            Funcionario funcionario = funcionarioOpt.get();
            System.out.println("✅ FUNCIONÁRIO encontrado!");
            System.out.println("   - ID: " + funcionario.getId());
            System.out.println("   - Nome: " + funcionario.getNomeCompleto());
            System.out.println("   - Role: " + funcionario.getRole());
            System.out.println("   - Ativo: " + funcionario.isEnabled());
            return funcionario;
        }
        System.out.println("❌ FUNCIONÁRIO não encontrado");

        // 3. Tentar buscar como CLIENTE
        System.out.println("🔍 Buscando como CLIENTE...");
        Optional<Cliente> clienteOpt = clienteRepository.findByUsername(username);
        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();
            System.out.println("✅ CLIENTE encontrado!");
            System.out.println("   - ID: " + cliente.getId());
            System.out.println("   - Nome: " + cliente.getNomeCompleto());
            System.out.println("   - Role: " + cliente.getRole());
            System.out.println("   - Ativo: " + cliente.isEnabled());
            return cliente;
        }
        System.out.println("❌ CLIENTE não encontrado");

        // 4. Nenhum usuário encontrado
        System.err.println("❌❌❌ ERRO: Usuário não encontrado em NENHUMA tabela!");
        System.err.println("Username buscado: '" + username + "'");

        throw new UsernameNotFoundException("Usuário não encontrado: " + username);
    }
}
