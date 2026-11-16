package com.example.sistema_prontuarios.web.dto;

import java.time.Instant;

/** Corrigir um resultado cria uma nova versao; a de maior numero e a vigente. */
public record ResultadoExameDto(
        Integer versao,
        String resultado,
        Instant dataResultado,
        String registradoPor,
        String tipoRegistrador) {
}
