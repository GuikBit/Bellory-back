package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.entity.User;
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
        // Cria o objeto de autenticação com o usuário e senha
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.username(), request.password());

        // O AuthenticationManager irá validar o usuário e senha.
        Authentication authentication = this.authenticationManager.authenticate(authenticationToken);

        // CORREÇÃO: Pega o objeto User completo da autenticação e passa para o TokenService
        var user = (User) authentication.getPrincipal();
        String token = tokenService.generateToken(user);

        return new LoginResponse(token);
    }
}
