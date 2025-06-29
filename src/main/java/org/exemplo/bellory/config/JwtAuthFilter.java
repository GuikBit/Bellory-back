package org.exemplo.bellory.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.exemplo.bellory.model.repository.users.UserRepository;
import org.exemplo.bellory.service.TokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public JwtAuthFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        //String path = request.getRequestURI();

//        // Ignorar rotas públicas
//        if (path.startsWith("/api/auth") ||
//                // Mude para "/api/funcionario/"
//                path.startsWith("/api/funcionario/") ||
//                path.startsWith("/api/servico") ||
//                path.startsWith("/api/pages") ||
//                path.startsWith("/api/test")) {
//            filterChain.doFilter(request, response);
//            return;
//        }

        String token = recoverToken(request);
        if (token != null) {
            String subject = tokenService.validateToken(token);
            if (subject != null && !subject.isEmpty()) {
                UserDetails user = userRepository.findByUsername(subject).orElse(null);
                if (user != null) {
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.replace("Bearer ", "");
    }
}
