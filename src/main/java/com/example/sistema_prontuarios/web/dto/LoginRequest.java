package com.example.sistema_prontuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** "identificador" e o e-mail do usuario ou, no caso do paciente, o CPF. */
public record LoginRequest(
        @NotBlank String identificador,
        @NotBlank @Size(max = 72) String senha) {
}
