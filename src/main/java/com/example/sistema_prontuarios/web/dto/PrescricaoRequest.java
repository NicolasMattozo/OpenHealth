package com.example.sistema_prontuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescricaoRequest(
        @NotBlank @Size(max = 200) String medicamento,
        @NotBlank @Size(max = 100) String dosagem,
        @Size(max = 50) String via,
        @Size(max = 100) String frequencia,
        Boolean visivelAoPaciente) {
}
