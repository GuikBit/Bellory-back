package org.exemplo.bellory.util;

public class CNPJUtil {

    /**
     * Remove formatação do CNPJ (pontos, barras e hífens)
     * Exemplo: 00.000.000/0001-00 -> 00000000000100
     */
    public static String removerFormatacao(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        return cnpj.replaceAll("[^0-9]", "");
    }

    /**
     * Formata CNPJ para exibição
     * Exemplo: 00000000000100 -> 00.000.000/0001-00
     */
    public static String formatarCNPJ(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.substring(0, 2) + "." +
                cnpj.substring(2, 5) + "." +
                cnpj.substring(5, 8) + "/" +
                cnpj.substring(8, 12) + "-" +
                cnpj.substring(12, 14);
    }

    /**
     * Valida se o CNPJ é válido (algoritmo oficial)
     */
    public static boolean validarCNPJ(String cnpj) {
        cnpj = removerFormatacao(cnpj);

        if (cnpj == null || cnpj.length() != 14) {
            return false;
        }

        // Verifica se todos os dígitos são iguais
        if (cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        try {
            // Calcula primeiro dígito verificador
            int soma = 0;
            int[] peso1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            for (int i = 0; i < 12; i++) {
                soma += Character.getNumericValue(cnpj.charAt(i)) * peso1[i];
            }
            int digito1 = (soma % 11 < 2) ? 0 : 11 - (soma % 11);

            // Calcula segundo dígito verificador
            soma = 0;
            int[] peso2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            for (int i = 0; i < 13; i++) {
                soma += Character.getNumericValue(cnpj.charAt(i)) * peso2[i];
            }
            int digito2 = (soma % 11 < 2) ? 0 : 11 - (soma % 11);

            // Verifica se os dígitos calculados conferem
            return digito1 == Character.getNumericValue(cnpj.charAt(12)) &&
                    digito2 == Character.getNumericValue(cnpj.charAt(13));

        } catch (Exception e) {
            return false;
        }
    }
}
