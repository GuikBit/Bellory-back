package org.exemplo.bellory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.assinatura.AcessoDTO;
import org.exemplo.bellory.model.dto.assinatura.AssinaturaStatusDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.assinatura.AssinaturaCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor que bloqueia acesso quando a Payment API informa {@code allowed=false}
 * para o customer da organizacao autenticada.
 *
 * <p>Fail-open deliberado: quando a Payment API está fora e não há cache stale,
 * {@link AssinaturaCacheService#getStatusByOrganizacao(Long)} retorna {@code bloqueado=false}
 * com {@code statusAssinatura=INDISPONIVEL}. Preferimos liberar o acesso do cliente legítimo
 * a bloquear tudo por indisponibilidade nossa.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssinaturaInterceptor implements HandlerInterceptor {

    private final AssinaturaCacheService assinaturaCacheService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        // Sem org no contexto = admin global ou request publico -> libera
        if (organizacaoId == null) {
            return true;
        }

        AssinaturaStatusDTO status = assinaturaCacheService.getStatusByOrganizacao(organizacaoId);

        if (status.isBloqueado()) {
            log.info("Acesso bloqueado para organizacao {} - situacao: {} - motivo: {}",
                    organizacaoId, status.getSituacao(), status.getMensagem());

            AcessoDTO acesso = AcessoDTO.builder()
                    .bloqueado(true)
                    .statusAssinatura(status.getStatusAssinatura())
                    .mensagem(status.getMensagem() != null
                            ? status.getMensagem()
                            : "Acesso bloqueado. Regularize sua assinatura para continuar.")
                    .build();

            ResponseAPI<AcessoDTO> body = ResponseAPI.<AcessoDTO>builder()
                    .success(false)
                    .message(acesso.getMensagem())
                    .dados(acesso)
                    .errorCode(HttpStatus.FORBIDDEN.value())
                    .build();

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return false;
        }

        return true;
    }
}
