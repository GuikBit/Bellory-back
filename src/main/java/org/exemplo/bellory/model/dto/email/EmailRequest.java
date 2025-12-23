package org.exemplo.bellory.model.dto.email;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    // Destinatários
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;

    // Assunto (opcional, pode vir do template)
    private String subject;

    // Template ou corpo HTML direto
    private org.exemplo.bellory.model.entity.email.EmailTemplate template;
    private String htmlBody; // Usar se não quiser template

    // Variáveis para substituir no template
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    // Anexos (futuro)
    private List<EmailAttachment> attachments;
}
