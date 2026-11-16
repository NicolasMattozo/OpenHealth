package com.example.sistema_prontuarios.web.dto;

import java.time.Instant;

/** "chaveRecurso" e um texto JSON (cnpj, cpf, ids, status HTTP, ip...); o frontend pode fazer JSON.parse. */
public record LogAuditoriaDto(
        Long id,
        Instant instante,
        Long idUsuario,
        String nomeUsuario,
        String acao,
        String tipoRecurso,
        String chaveRecurso) {
}
