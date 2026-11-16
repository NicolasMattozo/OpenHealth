package com.example.sistema_prontuarios.util;

/** Normalizacao e validacao de CNPJ, numerico ou alfanumerico (IN RFB 2.229/2024). */
public final class Cnpj {

    private static final int[] PESOS_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Cnpj() {
    }

    /** Tira mascara (. / -) e espacos e deixa em maiusculas. */
    public static String normalizar(String valor) {
        return valor == null ? null : valor.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
    }

    /** Recebe o CNPJ ja normalizado: 12 caracteres [0-9A-Z] + 2 digitos verificadores. */
    public static boolean valido(String cnpj) {
        if (cnpj == null || !cnpj.matches("[0-9A-Z]{12}[0-9]{2}")) {
            return false;
        }
        if (cnpj.chars().distinct().count() == 1) {
            return false; // 00000000000000, AAAAAAAAAAAAAA etc.
        }
        int dv1 = digito(cnpj.substring(0, 12), PESOS_DV1);
        int dv2 = digito(cnpj.substring(0, 12) + dv1, PESOS_DV2);
        return cnpj.charAt(12) - '0' == dv1 && cnpj.charAt(13) - '0' == dv2;
    }

    /** Modulo 11; cada caractere vale o seu codigo ASCII menos 48 (0-9 => 0-9, A-Z => 17-42). */
    private static int digito(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
