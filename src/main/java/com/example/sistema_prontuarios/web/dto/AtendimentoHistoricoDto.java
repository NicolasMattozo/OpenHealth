package com.example.sistema_prontuarios.web.dto;

import com.example.sistema_prontuarios.domain.enums.StatusAtendimento;
import java.time.Instant;

public record AtendimentoHistoricoDto(
        Long id,
        String cnpjInstituicao,
        String nomeInstituicao,
        String motivo,
        Instant dataHora,
        Instant dataFinalizacao,
        StatusAtendimento status,
        String nomeMedico) {
}
