package com.example.sistema_prontuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Classificacao de risco no protocolo de Manchester. "visivelAoPaciente" omitido = visivel. */
public record TriagemRequest(
        @NotBlank @Pattern(regexp = "VERMELHO|LARANJA|AMARELO|VERDE|AZUL") String classificacaoRisco,
        @Size(max = 2000) String sintomas,
        Boolean visivelAoPaciente) {
}
