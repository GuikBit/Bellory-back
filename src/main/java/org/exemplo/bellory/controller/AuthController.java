package org.exemplo.bellory.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.exemplo.bellory.model.dto.auth.*;
import org.exemplo.bellory.model.dto.clienteDTO.ClienteCreateDTO;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.users.User;
import org.exemplo.bellory.service.TokenService;
import org.exemplo.bellory.service.CustomUserDetailsService;
import org.exemplo.bellory.service.UserInfoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"https://bellory.vercel.app", "https://*.vercel.app", "http://localhost:*"})
public class AuthController {

    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final UserInfoService userInfoService;

    public AuthController(TokenService tokenService,
                          AuthenticationManager authenticationManager,
                          CustomUserDetailsService userDetailsService,
                          UserInfoService userInfoService) {
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.userInfoService = userInfoService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request,
                                   @RequestParam(value = "withStats", defaultValue = "false") boolean withStats,
                                   HttpServletRequest httpRequest) {
        try {
            // Tentativa de autenticação
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername().trim().toLowerCase(),
                            request.getPassword()
                    );

            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            var user = (User) authentication.getPrincipal();

            // Verificar se o usuário está ativo
            if (!user.isEnabled()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse(
                                "Conta desativada. Entre em contato com o administrador",
                                "ACCOUNT_DISABLED",
                                httpRequest.getRequestURI()
                        ));
            }

            // buscar organizacao
            Organizacao org = user.getOrganizacao();

            OrganizacaoInfoDTO organizacaoInfo = new OrganizacaoInfoDTO();
            organizacaoInfo.setId(org.getId());
            organizacaoInfo.setNome(org.getNomeFantasia());
            organizacaoInfo.setDtCadastro(org.getDtCadastro());
            organizacaoInfo.setNomeFantasia(org.getNomeFantasia());
//            organizacaoInfo.setPlano(org.getPlano());
            organizacaoInfo.setConfigSistema(org.getConfigSistema());
            organizacaoInfo.setAtivo(org.getAtivo());
//            organizacaoInfo.setLimitesPersonalizados(org.getLimitesPersonalizados());

            // Gerar token
            String token = tokenService.generateToken(user);
            LocalDateTime expiresAt = tokenService.getExpirationDateTime();

            // Construir informações completas do usuário
            UserInfoDTO userInfo;
            if (withStats) {
                userInfo = userInfoService.buildUserInfoWithStatistics(user);
            } else {
                userInfo = userInfoService.buildUserInfo(user);
            }

            LoginResponseDTO response = LoginResponseDTO.builder()
                    .success(true)
                    .message("Login realizado com sucesso")
                    .token(token)
                    .organizacao(organizacaoInfo)
                    .user(userInfo)
                    .expiresAt(expiresAt)
                    .build();

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(
                            "Credenciais inválidas. Verifique seu usuário e senha",
                            "INVALID_CREDENTIALS",
                            httpRequest.getRequestURI()
                    ));

        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(
                            "Conta desativada",
                            "ACCOUNT_DISABLED",
                            httpRequest.getRequestURI()
                    ));

        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(
                            "Usuário não encontrado",
                            "USER_NOT_FOUND",
                            httpRequest.getRequestURI()
                    ));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(
                            "Falha na autenticação",
                            "AUTHENTICATION_FAILED",
                            httpRequest.getRequestURI()
                    ));

        } catch (Exception e) {
            // Log do erro para debugging
            System.err.println("Erro inesperado no login: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(
                            "Erro interno do servidor. Tente novamente",
                            "INTERNAL_ERROR",
                            httpRequest.getRequestURI()
                    ));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader,
                                           HttpServletRequest httpRequest) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(
                                "Token não fornecido ou formato inválido",
                                "MISSING_TOKEN",
                                httpRequest.getRequestURI()
                        ));
            }

            String token = authHeader.replace("Bearer ", "");

            if (tokenService.isTokenExpired(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse(
                                "Token expirado",
                                "TOKEN_EXPIRED",
                                httpRequest.getRequestURI()
                        ));
            }

            String username = tokenService.validateToken(token);

            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse(
                                "Token inválido",
                                "INVALID_TOKEN",
                                httpRequest.getRequestURI()
                        ));
            }

            // Buscar informações atuais do usuário
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            Long userId = tokenService.getUserIdFromToken(token);

            // Determinar tipo de usuário
            String userType = determineUserType(username);

            TokenValidationResponseDTO response = TokenValidationResponseDTO.builder()
                    .valid(true)
                    .username(username)
                    .userId(userId)
                    .userType(userType)
                    .roles(userDetails.getAuthorities().stream()
                            .map(auth -> auth.getAuthority())
                            .collect(Collectors.toList()))
                    .expiresAt(tokenService.getExpirationDateTime())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(
                            "Erro ao validar token",
                            "VALIDATION_ERROR",
                            httpRequest.getRequestURI()
                    ));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request,
                                          HttpServletRequest httpRequest) {
        try {
            String newToken = tokenService.refreshToken(request.getToken());
            LocalDateTime expiresAt = tokenService.getExpirationDateTime();

            RefreshTokenResponseDTO response = RefreshTokenResponseDTO.builder()
                    .success(true)
                    .message("Token renovado com sucesso")
                    .newToken(newToken)
                    .expiresAt(expiresAt)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(
                            "Não foi possível renovar o token: " + e.getMessage(),
                            "REFRESH_FAILED",
                            httpRequest.getRequestURI()
                    ));
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        // Aqui você poderia implementar uma blacklist de tokens se necessário
        LoginResponseDTO response = LoginResponseDTO.builder()
                .success(true)
                .message("Logout realizado com sucesso")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader,
                                            @RequestParam(value = "withStats", defaultValue = "false") boolean withStats,
                                            HttpServletRequest httpRequest) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(
                                "Token não fornecido",
                                "MISSING_TOKEN",
                                httpRequest.getRequestURI()
                        ));
            }

            String token = authHeader.replace("Bearer ", "");
            String username = tokenService.validateToken(token);

            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse(
                                "Token inválido",
                                "INVALID_TOKEN",
                                httpRequest.getRequestURI()
                        ));
            }

            // Buscar usuário e construir informações completas
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            User user = (User) userDetails;

            UserInfoDTO userInfo;
            if (withStats) {
                userInfo = userInfoService.buildUserInfoWithStatistics(user);
            } else {
                userInfo = userInfoService.buildUserInfo(user);
            }

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(
                            "Erro ao buscar informações do usuário",
                            "USER_INFO_ERROR",
                            httpRequest.getRequestURI()
                    ));
        }
    }

    private String determineUserType(String username) {
        // Aqui você pode implementar lógica para determinar o tipo baseado no username
        // ou consultar os repositories
        return "USER"; // Fallback
    }

    private ErrorResponseDTO createErrorResponse(String message, String errorCode, String path) {
        return ErrorResponseDTO.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}
