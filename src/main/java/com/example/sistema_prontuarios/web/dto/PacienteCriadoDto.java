package com.example.sistema_prontuarios.web.dto;

/** "senhaTemporaria" aparece uma unica vez, para a recepcao entregar ao paciente. */
public record PacienteCriadoDto(PacienteDto paciente, String senhaTemporaria) {
}
