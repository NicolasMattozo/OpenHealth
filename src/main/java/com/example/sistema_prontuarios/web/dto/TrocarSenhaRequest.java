package com.example.sistema_prontuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequest(
        @NotBlank @Size(max = 72) String senhaAtual,
        @NotBlank @Size(min = 8, max = 72) String novaSenha) {
}
