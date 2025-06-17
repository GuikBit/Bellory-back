package org.exemplo.bellory.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * Este é um endpoint PÚBLICO.
     * Qualquer pessoa pode aceder a este endpoint, mesmo sem um token JWT,
     * porque a rota "/api/test/**" foi permitida no SecurityConfig.
     * @return Uma mensagem pública.
     */
    @GetMapping
    public String getPublicData() {
        return "Olá! Esta é uma mensagem pública que não precisa de login para ser acedida.";
    }

}
