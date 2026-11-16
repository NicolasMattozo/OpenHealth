package com.example.sistema_prontuarios.web.dto;

import java.util.List;

public record ProntuarioDetalhadoDto(
        AtendimentoHistoricoDto atendimento,
        String cpfPaciente,
        String nomePaciente,
        List<RegistroClinicoDto> registros) {
}
