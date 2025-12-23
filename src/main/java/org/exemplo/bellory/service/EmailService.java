package org.exemplo.bellory.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.email.EmailRequest;
import org.exemplo.bellory.model.entity.email.EmailTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${email.from.address}")
    private String fromAddress;

    @Value("${email.from.name}")
    private String fromName;

    /**
     * Envia e-mail usando template
     */
    @Async
    public void enviarEmail(EmailRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Configurar remetente
            helper.setFrom(fromAddress, fromName);

            // Configurar destinatários
            if (request.getTo() != null && !request.getTo().isEmpty()) {
                helper.setTo(request.getTo().toArray(new String[0]));
            }

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().toArray(new String[0]));
            }

            // Definir assunto
            String subject = request.getSubject();
            if (subject == null && request.getTemplate() != null) {
                subject = request.getTemplate().getSubject();
            }
            helper.setSubject(subject);

            // Processar corpo do e-mail
            String htmlContent;
            if (request.getTemplate() != null) {
                // Usar template
                htmlContent = processarTemplate(request.getTemplate(), request.getVariables());
            } else if (request.getHtmlBody() != null) {
                // Usar HTML direto
                htmlContent = request.getHtmlBody();
            } else {
                throw new IllegalArgumentException("É necessário fornecer um template ou htmlBody");
            }

            helper.setText(htmlContent, true);

            // Enviar e-mail
            mailSender.send(message);

            log.info("E-mail enviado com sucesso para: {}", request.getTo());

        } catch (Exception e) {
            log.error("Erro ao enviar e-mail para: {}", request.getTo(), e);
            throw new RuntimeException("Falha ao enviar e-mail", e);
        }
    }

    /**
     * Processa template Thymeleaf com variáveis
     */
    private String processarTemplate(EmailTemplate template, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process("emails/" + template.getTemplateName(), context);
    }

    /**
     * Envia e-mail simples (sem template)
     */
    @Async
    public void enviarEmailSimples(String to, String subject, String body) {
        EmailRequest request = EmailRequest.builder()
                .to(List.of(to))
                .subject(subject)
                .htmlBody(body)
                .build();

        enviarEmail(request);
    }

    /**
     * Envia e-mail usando template específico
     */
    @Async
    public void enviarEmailComTemplate(
            List<String> destinatarios,
            EmailTemplate template,
            Map<String, Object> variables) {

        EmailRequest request = EmailRequest.builder()
                .to(destinatarios)
                .template(template)
                .variables(variables)
                .build();

        enviarEmail(request);
    }
}
