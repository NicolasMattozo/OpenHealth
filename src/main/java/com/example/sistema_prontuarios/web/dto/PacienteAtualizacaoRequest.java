package com.example.sistema_prontuarios.web.dto;

import jakarta.validation.constraints.Size;

/** Dados de contato; nome, CPF e data de nascimento nao mudam por aqui. */
public record PacienteAtualizacaoRequest(
        @Size(max = 20) String telefone,
        @Size(max = 300) String endereco) {
}
