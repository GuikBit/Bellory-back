package org.exemplo.bellory.service.questionario;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Helper estatico para decodificar e validar imagens de assinatura digital
 * recebidas via base64 data URL do front-end (canvas.toDataURL).
 *
 * Aceita apenas PNG e SVG. Limite de 200KB de bytes brutos descodificados.
 * Valida magic numbers para impedir uploads disfarçados, e sanitiza SVG removendo
 * scripts/handlers que poderiam virar XSS no PDF ou no front.
 */
public final class AssinaturaImagemValidator {

    public static final int LIMITE_BYTES = 200 * 1024; // 200KB

    private static final String PREFIX_PNG = "data:image/png;base64,";
    private static final String PREFIX_SVG = "data:image/svg+xml;base64,";

    /**
     * Magic number do PNG: 89 50 4E 47 0D 0A 1A 0A
     */
    private static final byte[] PNG_MAGIC = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    /**
     * Padroes para detectar conteudo perigoso em SVG (case-insensitive).
     * Cobre: script tags, event handlers (on*=), javascript: URIs, foreignObject embebido.
     */
    private static final Pattern SVG_SCRIPT = Pattern.compile("<script\\b[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SVG_SCRIPT_SELF_CLOSING = Pattern.compile("<script\\b[^/>]*/>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_EVENT_HANDLER = Pattern.compile("\\son\\w+\\s*=\\s*\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_EVENT_HANDLER_SINGLE = Pattern.compile("\\son\\w+\\s*=\\s*'[^']*'", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_JAVASCRIPT_URI = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_FOREIGN_OBJECT = Pattern.compile("<foreignObject\\b[^>]*>.*?</foreignObject>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private AssinaturaImagemValidator() {
        // utilitario
    }

    public enum Formato {
        PNG("png", "image/png"),
        SVG("svg", "image/svg+xml");

        private final String extensao;
        private final String contentType;

        Formato(String extensao, String contentType) {
            this.extensao = extensao;
            this.contentType = contentType;
        }

        public String getExtensao() { return extensao; }
        public String getContentType() { return contentType; }
    }

    public static class Resultado {
        private final byte[] bytes;
        private final Formato formato;

        public Resultado(byte[] bytes, Formato formato) {
            this.bytes = bytes;
            this.formato = formato;
        }

        public byte[] getBytes() { return bytes; }
        public Formato getFormato() { return formato; }
    }

    /**
     * Decodifica e valida o data URL de uma assinatura.
     *
     * @param dataUrl payload do front no formato {@code data:image/<png|svg+xml>;base64,...}
     * @return bytes brutos descodificados + formato detectado
     * @throws IllegalArgumentException se o payload for invalido (formato, tamanho ou conteudo)
     */
    public static Resultado decodificarEValidar(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new IllegalArgumentException("Payload da assinatura está vazio.");
        }

        Formato formato;
        String base64;

        if (dataUrl.startsWith(PREFIX_PNG)) {
            formato = Formato.PNG;
            base64 = dataUrl.substring(PREFIX_PNG.length());
        } else if (dataUrl.startsWith(PREFIX_SVG)) {
            formato = Formato.SVG;
            base64 = dataUrl.substring(PREFIX_SVG.length());
        } else {
            throw new IllegalArgumentException(
                    "Formato de assinatura invalido. Aceito apenas data:image/png;base64 ou data:image/svg+xml;base64.");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Payload base64 da assinatura está corrompido.", e);
        }

        if (bytes.length == 0) {
            throw new IllegalArgumentException("Conteudo da assinatura está vazio.");
        }

        if (bytes.length > LIMITE_BYTES) {
            throw new IllegalArgumentException(
                    "Assinatura excede o tamanho maximo permitido (" + (LIMITE_BYTES / 1024) + "KB).");
        }

        if (formato == Formato.PNG) {
            validarMagicPng(bytes);
            return new Resultado(bytes, formato);
        }

        // SVG
        byte[] sanitizados = sanitizarSvg(bytes);
        return new Resultado(sanitizados, formato);
    }

    private static void validarMagicPng(byte[] bytes) {
        if (bytes.length < PNG_MAGIC.length) {
            throw new IllegalArgumentException("Assinatura PNG invalida (cabecalho ausente).");
        }
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (bytes[i] != PNG_MAGIC[i]) {
                throw new IllegalArgumentException(
                        "Assinatura PNG invalida (magic number nao confere).");
            }
        }
    }

    /**
     * Verifica que o conteudo parece SVG e remove construcoes potencialmente perigosas.
     * Retorna os bytes ja sanitizados.
     */
    private static byte[] sanitizarSvg(byte[] bytes) {
        String conteudo = new String(bytes, StandardCharsets.UTF_8).trim();

        if (!conteudo.startsWith("<svg") && !conteudo.startsWith("<?xml")) {
            throw new IllegalArgumentException(
                    "Assinatura SVG invalida (deve comecar com <svg ou <?xml).");
        }

        String sanitizado = conteudo;
        sanitizado = SVG_SCRIPT.matcher(sanitizado).replaceAll("");
        sanitizado = SVG_SCRIPT_SELF_CLOSING.matcher(sanitizado).replaceAll("");
        sanitizado = SVG_FOREIGN_OBJECT.matcher(sanitizado).replaceAll("");
        sanitizado = SVG_EVENT_HANDLER.matcher(sanitizado).replaceAll("");
        sanitizado = SVG_EVENT_HANDLER_SINGLE.matcher(sanitizado).replaceAll("");
        sanitizado = SVG_JAVASCRIPT_URI.matcher(sanitizado).replaceAll("");

        return sanitizado.getBytes(StandardCharsets.UTF_8);
    }
}
