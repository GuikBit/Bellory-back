package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Teste", description = "Endpoints de teste do sistema")
public class TestController {

    /**
     * Este é um endpoint PÚBLICO.
     * Qualquer pessoa pode aceder a este endpoint, mesmo sem um token JWT,
     * porque a rota "/api/test/**" foi permitida no SecurityConfig.
     * @return Uma mensagem pública.
     */
    @GetMapping
    @Operation(summary = "Endpoint público de teste")
    public String getPublicData() {
        return "Olá! Esta é uma mensagem pública que não precisa de login para ser acedida.";
    }

}
