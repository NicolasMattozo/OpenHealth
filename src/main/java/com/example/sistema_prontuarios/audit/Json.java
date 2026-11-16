package com.example.sistema_prontuarios.audit;

import java.util.Collection;
import java.util.Map;

/** Escreve um mapa simples como JSON (texto, numero, booleano, lista, nulo), sem depender de biblioteca. */
final class Json {

    private static final int MAX_TEXTO = 200;

    private Json() {
    }

    static String escrever(Map<String, Object> mapa) {
        StringBuilder json = new StringBuilder("{");
        boolean primeiro = true;
        for (Map.Entry<String, Object> campo : mapa.entrySet()) {
            if (!primeiro) {
                json.append(',');
            }
            primeiro = false;
            json.append('"').append(escapar(campo.getKey())).append("\":");
            valor(json, campo.getValue());
        }
        return json.append('}').toString();
    }

    private static void valor(StringBuilder json, Object valor) {
        if (valor == null) {
            json.append("null");
        } else if (valor instanceof Number || valor instanceof Boolean) {
            json.append(valor);
        } else if (valor instanceof Collection<?> colecao) {
            json.append('[');
            boolean primeiro = true;
            for (Object item : colecao) {
                if (!primeiro) {
                    json.append(',');
                }
                primeiro = false;
                valor(json, item);
            }
            json.append(']');
        } else {
            json.append('"').append(escapar(valor.toString())).append('"');
        }
    }

    /** Escapa aspas, barras e caracteres de controle; textos muito longos sao cortados. */
    static String escapar(String texto) {
        String t = texto.length() > MAX_TEXTO ? texto.substring(0, MAX_TEXTO) : texto;
        StringBuilder saida = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            switch (c) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                case '\n' -> saida.append("\\n");
                case '\r' -> saida.append("\\r");
                case '\t' -> saida.append("\\t");
                default -> {
                    if (c < 0x20) {
                        saida.append(String.format("\\u%04x", (int) c));
                    } else {
                        saida.append(c);
                    }
                }
            }
        }
        return saida.toString();
    }
}
