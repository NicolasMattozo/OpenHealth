package com.example.sistema_prontuarios.util;

/** Normalizacao e validacao de CPF. */
public final class Cpf {

    private Cpf() {
    }

    /** Deixa so os digitos. */
    public static String normalizar(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    /** Recebe o CPF ja normalizado (11 digitos). */
    public static boolean valido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}")) {
            return false;
        }
        if (cpf.chars().distinct().count() == 1) {
            return false; // 11111111111 etc.
        }
        return digito(cpf, 9) == cpf.charAt(9) - '0' && digito(cpf, 10) == cpf.charAt(10) - '0';
    }

    /** Digito verificador calculado sobre os "quantos" primeiros digitos (pesos de quantos+1 ate 2). */
    private static int digito(String cpf, int quantos) {
        int soma = 0;
        for (int i = 0; i < quantos; i++) {
            soma += (cpf.charAt(i) - '0') * (quantos + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
