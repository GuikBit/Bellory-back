package org.exemplo.bellory.util;

import java.text.Normalizer;
import java.util.Random;
import java.util.regex.Pattern;

public class SlugUtil {

    private static final String CARACTERES_ALEATORIOS = "abcdefghijklmnopqrstuvwxyz";
    private static final int TAMANHO_CODIGO = 5;
    private static final Random random = new Random();

    /**
     * Gera um slug a partir do nome fantasia
     * Exemplo: "Salão de Beleza Maria" -> "salao-de-beleza-maria-xk7pm"
     */
    public static String gerarSlug(String nomeFantasia) {
        if (nomeFantasia == null || nomeFantasia.isBlank()) {
            return gerarCodigoAleatorio();
        }

        String slugBase = limparTexto(nomeFantasia);
        String codigoUnico = gerarCodigoAleatorio();

        return slugBase + "-" + codigoUnico;
    }

    /**
     * Limpa o texto removendo:
     * - Acentuação
     * - Números
     * - Caracteres especiais
     * - Espaços múltiplos
     * - Converte para lowercase
     * - Substitui espaços por hífens
     */
    public static String limparTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }

        // Remove acentuação (normalização NFD)
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        semAcentos = pattern.matcher(semAcentos).replaceAll("");

        // Remove números
        String semNumeros = semAcentos.replaceAll("\\d", "");

        // Remove caracteres especiais (mantém apenas letras e espaços)
        String apenasLetras = semNumeros.replaceAll("[^a-zA-Z\\s]", "");

        // Converte para lowercase
        String lowercase = apenasLetras.toLowerCase().trim();

        // Remove espaços múltiplos
        String espacosNormalizados = lowercase.replaceAll("\\s+", " ");

        // Substitui espaços por hífens
        String comHifens = espacosNormalizados.replace(" ", "-");

        // Remove hífens no início ou fim
        comHifens = comHifens.replaceAll("^-+|-+$", "");

        // Se ficou vazio, retorna string vazia
        return comHifens.isEmpty() ? "" : comHifens;
    }

    /**
     * Gera um código aleatório de 5 letras
     * Exemplo: "xk7pm", "abc12", "zyx99"
     */
    public static String gerarCodigoAleatorio() {
        StringBuilder codigo = new StringBuilder(TAMANHO_CODIGO);

        for (int i = 0; i < TAMANHO_CODIGO; i++) {
            int index = random.nextInt(CARACTERES_ALEATORIOS.length());
            codigo.append(CARACTERES_ALEATORIOS.charAt(index));
        }

        return codigo.toString();
    }

    /**
     * Valida se um slug está no formato correto
     */
    public static boolean validarSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        // Formato: letras-minúsculas-com-hífens-e-5-letras-no-final
        return slug.matches("^[a-z]+(-[a-z]+)*-[a-z]{5}$");
    }
}
