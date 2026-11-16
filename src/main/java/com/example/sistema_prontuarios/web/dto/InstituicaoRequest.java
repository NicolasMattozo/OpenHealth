package com.example.sistema_prontuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O CNPJ pode vir com ou sem mascara. */
public record InstituicaoRequest(
        @NotBlank @Size(max = 200) String nome,
        @NotBlank @Size(max = 18) String cnpj,
        @Size(max = 50) String tipo,
        @Size(max = 300) String endereco) {
}
