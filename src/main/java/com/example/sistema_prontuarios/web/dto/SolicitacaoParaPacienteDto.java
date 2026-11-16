package com.example.sistema_prontuarios.web.dto;

import com.example.sistema_prontuarios.domain.enums.StatusSolicitacao;
import java.time.Instant;

/** Solicitacao de acesso vista pelo paciente: quem pede (instituicao e funcionario) e para que. */
public record SolicitacaoParaPacienteDto(
        Long id,
        String cnpjInstituicao,
        String nomeInstituicao,
        String tipoInstituicao,
        String finalidade,
        String solicitadaPor,
        StatusSolicitacao status,
        Instant dataSolicitacao,
        Instant dataDecisao,
        Instant dataRevogacao) {
}
