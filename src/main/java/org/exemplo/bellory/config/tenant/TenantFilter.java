package org.exemplo.bellory.config.tenant;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Filtro para identificar e configurar o tenant baseado no subdomínio.
 * Executa antes de qualquer outro filtro para garantir que o contexto do tenant
 * esteja disponível em toda a requisição.
 */
@Component
@Order(1) // Executa primeiro
public class TenantFilter implements Filter {

    private static final Pattern SUBDOMAIN_PATTERN = Pattern.compile("^([a-zA-Z0-9-]+)\\.bellory\\.com\\.br$");
    private static final String LOCALHOST_PATTERN = "localhost";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            String tenantId = extractTenantFromHost(httpRequest);

            if (tenantId != null) {
                TenantContext.setCurrentTenant(tenantId);
                // Adiciona o tenant como atributo da requisição para acesso alternativo
                httpRequest.setAttribute("tenant_id", tenantId);
            } else {
                // Para desenvolvimento local ou quando não há subdomínio válido
                // você pode definir um tenant padrão ou retornar erro
                handleInvalidTenant(httpRequest, httpResponse);
            }

            chain.doFilter(request, response);

        } finally {
            // Sempre limpa o contexto ao final da requisição
            TenantContext.clear();
        }
    }

    /**
     * Extrai o tenant_id do header Host da requisição.
     * @param request A requisição HTTP
     * @return O tenant_id extraído ou null se não for válido
     */
    private String extractTenantFromHost(HttpServletRequest request) {
        String host = request.getHeader("Host");

        if (host == null || host.trim().isEmpty()) {
            return null;
        }

        // Remove porta se presente (ex: localhost:8080 -> localhost)
        host = host.split(":")[0].toLowerCase();

        // Para desenvolvimento local
        if (host.contains(LOCALHOST_PATTERN)) {
            // Você pode extrair o tenant de um parâmetro ou header adicional em desenvolvimento
            String devTenant = request.getHeader("X-Tenant-ID");
            return devTenant != null ? devTenant : "demo"; // tenant padrão para desenvolvimento
        }

        // Para produção - extrai o subdomínio
        var matcher = SUBDOMAIN_PATTERN.matcher(host);
        if (matcher.matches()) {
            return matcher.group(1); // Retorna o subdomínio
        }

        return null;
    }

    /**
     * Lida com requisições sem tenant válido.
     * @param request A requisição HTTP
     * @param response A resposta HTTP
     */
    private void handleInvalidTenant(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String path = request.getRequestURI();

        // Permite algumas rotas sem tenant (ex: health check, documentação)
        if (isPublicPath(path)) {
            TenantContext.setCurrentTenant("public");
            return;
        }

        // Para desenvolvimento, define um tenant padrão
        if (isDevEnvironment()) {
            TenantContext.setCurrentTenant("demo");
            return;
        }

        // Em produção, retorna erro para requisições sem tenant válido
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Invalid or missing tenant\",\"message\":\"Please access via a valid subdomain (e.g., yourname.bellory.com.br)\"}");
    }

    /**
     * Verifica se o path é público (não requer tenant).
     * @param path O path da requisição
     * @return true se for um path público
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/health") ||
                path.equals("/");
    }

    /**
     * Verifica se está em ambiente de desenvolvimento.
     * @return true se for ambiente de desenvolvimento
     */
    private boolean isDevEnvironment() {
        String env = System.getProperty("spring.profiles.active", "dev");
        return env.contains("dev") || env.contains("local");
    }
}
