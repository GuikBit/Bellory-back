package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.entity.users.User;
import org.exemplo.bellory.model.repository.users.UserRepository; // Importar o repositório
import org.exemplo.bellory.service.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails; // Importar UserDetails
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository; // Adicionar o repositório

    public AuthController(TokenService tokenService, AuthenticationManager authenticationManager, UserRepository userRepository) { // Adicionar no construtor
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository; // Atribuir
    }

    // DTO (Data Transfer Object) para receber os dados do login
    public record LoginRequest(String username, String password) {}
    // DTO para retornar o token
    public record LoginResponse(String token) {}

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        // Cria o objeto de autenticação com o usuário e senha
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.username(), request.password());

        // O AuthenticationManager irá validar o usuário e senha.
        Authentication authentication = this.authenticationManager.authenticate(authenticationToken);

        // --- CORREÇÃO APLICADA AQUI ---
        // 1. Pega o UserDetails do Spring Security que está no "principal"
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // 2. Usa o username para buscar a SUA entidade User completa no banco
        var user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado após autenticação")); // Lança exceção se não encontrar

        // 3. Gera o token com a sua entidade User
        String token = tokenService.generateToken(user);

        return new LoginResponse(token);
    }
}
