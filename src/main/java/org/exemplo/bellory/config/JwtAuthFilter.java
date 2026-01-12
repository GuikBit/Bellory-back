package org.exemplo.bellory.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.ApiKeyUserInfo;
import org.exemplo.bellory.service.ApiKeyService;
import org.exemplo.bellory.service.CustomUserDetailsService;
import org.exemplo.bellory.service.TokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final CustomUserDetailsService userDetailsService;
    private final ApiKeyService apiKeyService;

    public JwtAuthFilter(TokenService tokenService, CustomUserDetailsService userDetailsService, ApiKeyService apiKeyService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Adicionar headers CORS manualmente no filtro
        String origin = request.getHeader("Origin");
        if (origin != null && (
                origin.equals("https://bellory.vercel.app") ||
                        origin.matches("https://.*\\.vercel\\.app") ||
                        origin.matches("http://localhost:\\d+") ||
                        origin.matches("https://localhost:\\d+")
        )) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }

        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, Origin, X-Requested-With");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");

        // Se for requisição OPTIONS (preflight), responder imediatamente
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        try {
            // NOVA LÓGICA: Tentar API Key primeiro
            String apiKey = request.getHeader("X-API-Key");

            if (apiKey != null) {
                // Autenticação via API Key
                ApiKeyUserInfo userInfo = apiKeyService.validateApiKey(apiKey);

                if (userInfo != null) {
                    // Configurar contexto do tenant
                    TenantContext.setContext(
                            userInfo.getOrganizacaoId(),
                            userInfo.getUserId(),
                            userInfo.getUsername(),
                            userInfo.getRole()
                    );

                    // Carregar UserDetails usando o CustomUserDetailsService
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userInfo.getUsername());

                    if (userDetails != null && userDetails.isEnabled()) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }

                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // Autenticação JWT (código original)
            String token = recoverToken(request);

            if (token != null) {
                String subject = tokenService.validateToken(token);

                if (subject != null && !subject.isEmpty()) {
                    Long organizacaoId = tokenService.getOrganizacaoIdFromToken(token);
                    Long userId = tokenService.getUserIdFromToken(token);
                    String role = tokenService.getRoleFromToken(token);

                    TenantContext.setContext(organizacaoId, userId, subject, role);

                    UserDetails user = userDetailsService.loadUserByUsername(subject);
                    if (user != null) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities()
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.replace("Bearer ", "");
    }
}