package org.exemplo.bellory.model.entity.email;

import lombok.Getter;

@Getter
public enum EmailTemplate {

    BEM_VINDO_ORGANIZACAO(
            "bem-vindo-organizacao",
            "Bem-vindo à Bellory!"
    ),

    RESETAR_SENHA(
            "resetar-senha",
            "Recuperação de Senha - Bellory"
    ),

    CONFIRMACAO_CADASTRO(
            "confirmacao-cadastro",
            "Confirme seu cadastro - Bellory"
    ),

    NOTIFICACAO_AGENDAMENTO(
            "notificacao-agendamento",
            "Novo Agendamento - Bellory"
    ),

    LEMBRETE_AGENDAMENTO(
            "lembrete-agendamento",
            "Lembrete: Agendamento Próximo - Bellory"
    );

    private final String templateName;
    private final String subject;

    EmailTemplate(String templateName, String subject) {
        this.templateName = templateName;
        this.subject = subject;
    }
}
