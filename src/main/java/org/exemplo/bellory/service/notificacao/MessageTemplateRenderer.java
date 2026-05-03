package org.exemplo.bellory.service.notificacao;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Renderizador unificado de templates de mensagem (WhatsApp/email/etc).
 * Substitui placeholders no formato {{nome_variavel}} pelos valores do map.
 *
 * Mantido deliberadamente simples: cada caller (scheduler, anamnese, fila de
 * espera, futuras automacoes) monta seu proprio map de variaveis. O renderer
 * so faz a substituicao de string. Placeholders sem valor no map permanecem
 * intactos no resultado, o que torna obvio quando uma variavel esta faltando.
 */
@Component
public class MessageTemplateRenderer {

    public String render(String template, Map<String, String> variaveis) {
        if (template == null || template.isBlank()) {
            return "";
        }
        if (variaveis == null || variaveis.isEmpty()) {
            return template;
        }
        String resultado = template;
        for (Map.Entry<String, String> e : variaveis.entrySet()) {
            String valor = e.getValue() != null ? e.getValue() : "";
            resultado = resultado.replace("{{" + e.getKey() + "}}", valor);
        }
        return resultado;
    }
}
