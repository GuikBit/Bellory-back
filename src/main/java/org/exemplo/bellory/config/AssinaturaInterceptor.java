package org.exemplo.bellory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.assinatura.AcessoDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.assinatura.AssinaturaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssinaturaInterceptor implements HandlerInterceptor {

    private final AssinaturaService assinaturaService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        // Sem org no contexto = admin ou request publico -> libera
        if (organizacaoId == null) {
            return true;
        }

        AcessoDTO acesso = assinaturaService.verificarAcessoPermitido(organizacaoId);

        if (acesso.isBloqueado()) {
            log.info("Acesso bloqueado para organizacao {} - status: {}", organizacaoId, acesso.getStatusAssinatura());

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
