package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.entity.email.EmailTemplate;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
@Slf4j
@Tag(name = "E-mail", description = "Envio de e-mails e testes")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @Operation(summary = "Enviar e-mail de teste")
    @PostMapping("/teste")
    public ResponseEntity<ResponseAPI<String>> enviarEmailTeste(
            @RequestParam String destinatario) {

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("nomeOrganizacao", "Empresa Teste");
            variables.put("razaoSocial", "Empresa Teste LTDA");
            variables.put("cnpj", "00.000.000/0001-00");
            variables.put("slug", "empresa-teste-abc12");
            variables.put("email", destinatario);
            variables.put("username", "admin");
            variables.put("emailAdmin", destinatario);
            variables.put("urlSistema", "http://localhost:8081");

            emailService.enviarEmailComTemplate(
                    List.of(destinatario),
                    EmailTemplate.BEM_VINDO_ORGANIZACAO,
                    variables
            );

            return ResponseEntity.ok(ResponseAPI.<String>builder()
                    .success(true)
                    .message("E-mail de teste enviado com sucesso!")
                    .dados("Verifique a caixa de entrada de: " + destinatario)
                    .build());

        } catch (Exception e) {
            log.error("Erro ao enviar e-mail de teste", e);
            return ResponseEntity.status(500)
                    .body(ResponseAPI.<String>builder()
                            .success(false)
                            .message("Erro ao enviar e-mail: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
