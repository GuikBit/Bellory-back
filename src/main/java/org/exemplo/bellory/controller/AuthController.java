package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.entity.users.User;
import org.exemplo.bellory.service.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    // O UserRepository foi removido daqui.

    // O construtor foi atualizado para remover a injeção do UserRepository.
    public AuthController(TokenService tokenService, AuthenticationManager authenticationManager) {
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    // DTO (Data Transfer Object) para receber os dados do login
    public record LoginRequest(String username, String password) {}
    // DTO para retornar o token
    public record LoginResponse(String token) {}

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        // 1. Cria o objeto de autenticação com o usuário e senha.
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.username(), request.password());

        // 2. O AuthenticationManager (usando o seu CustomUserDetailsService) valida o usuário e senha.
        Authentication authentication = this.authenticationManager.authenticate(authenticationToken);

        // --- LÓGICA ATUALIZADA ---
        // 3. Após a autenticação, o "principal" é a própria entidade User que foi carregada.
        //    Fazemos um cast seguro para o nosso tipo User.
        var user = (User) authentication.getPrincipal();

        // 4. Geramos o token JWT diretamente com o objeto User completo.
        String token = tokenService.generateToken(user);

        return new LoginResponse(token);
    }
}
