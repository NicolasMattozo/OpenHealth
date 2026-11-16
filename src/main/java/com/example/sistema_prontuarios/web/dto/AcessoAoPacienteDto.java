package com.example.sistema_prontuarios.web.dto;

import java.time.Instant;

/** Algo que outra pessoa fez com os dados do paciente. "status" e o da resposta (403 = tentativa negada). */
public record AcessoAoPacienteDto(
        Instant instante,
        String acao,
        String ator,
        String instituicao,
        int status,
        boolean entreInstituicoes) {
}
