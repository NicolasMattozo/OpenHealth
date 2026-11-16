package com.example.sistema_prontuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosticoRequest(
        @NotBlank @Size(max = 2000) String descricao,
        @Size(max = 10) String cid,
        Boolean visivelAoPaciente) {
}
