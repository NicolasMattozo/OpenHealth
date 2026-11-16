package com.example.sistema_prontuarios.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** O CPF pode vir com ou sem mascara. A data vai no formato 1990-05-20. */
public record PacienteRequest(
        @NotBlank @Size(max = 200) String nome,
        @NotBlank @Size(max = 14) String cpf,
        @NotBlank @Email @Size(max = 200) String email,
        @NotNull @Past LocalDate dataNascimento,
        @Size(max = 20) String telefone,
        @Size(max = 300) String endereco) {
}
