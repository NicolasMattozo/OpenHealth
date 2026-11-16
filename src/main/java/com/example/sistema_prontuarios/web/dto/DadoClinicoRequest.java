package com.example.sistema_prontuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ex.: tipo "Pressao arterial", valor "120/80 mmHg". */
public record DadoClinicoRequest(
        @NotBlank @Size(max = 60) String tipo,
        @NotBlank @Size(max = 500) String valor,
        Boolean visivelAoPaciente) {
}
