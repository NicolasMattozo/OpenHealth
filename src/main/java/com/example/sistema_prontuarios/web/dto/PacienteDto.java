package com.example.sistema_prontuarios.web.dto;

import java.time.LocalDate;

public record PacienteDto(
        String cpf,
        String nome,
        String email,
        LocalDate dataNascimento,
        String telefone,
        String endereco) {
}
