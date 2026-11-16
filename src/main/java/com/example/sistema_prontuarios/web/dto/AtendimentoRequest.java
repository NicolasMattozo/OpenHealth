package com.example.sistema_prontuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtendimentoRequest(
        @NotBlank @Size(max = 14) String cpfPaciente,
        @NotBlank @Size(max = 300) String motivo) {
}
