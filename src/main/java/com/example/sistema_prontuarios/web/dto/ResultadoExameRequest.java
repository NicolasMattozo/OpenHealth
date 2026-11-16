package com.example.sistema_prontuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResultadoExameRequest(@NotBlank @Size(max = 5000) String resultado) {
}
