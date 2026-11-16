package com.example.sistema_prontuarios.web.dto;

import com.example.sistema_prontuarios.domain.enums.StatusSolicitacao;
import java.time.Instant;

public record SolicitacaoAcessoDto(
        Long id,
        String cpfPaciente,
        String nomePaciente,
        String cnpjInstituicao,
        String finalidade,
        StatusSolicitacao status,
        Instant dataSolicitacao,
        Instant dataDecisao,
        Instant dataRevogacao,
        String nomeRecepcionista) {
}
