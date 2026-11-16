package com.example.sistema_prontuarios.web.dto;

import com.example.sistema_prontuarios.domain.enums.StatusAtendimento;
import java.time.Instant;

public record AtendimentoDto(
        Long id,
        String cpfPaciente,
        String nomePaciente,
        String motivo,
        Instant dataHora,
        Instant dataFinalizacao,
        StatusAtendimento status,
        Long idSolicitacao,
        String nomeRecepcionista,
        String nomeMedico) {
}
