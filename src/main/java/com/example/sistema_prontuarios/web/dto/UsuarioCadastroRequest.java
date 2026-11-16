package com.example.sistema_prontuarios.web.dto;

import com.example.sistema_prontuarios.domain.enums.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** Os dados do conselho (CRM, COREN) so sao exigidos para medico e enfermeiro. */
public record UsuarioCadastroRequest(
        @NotBlank @Size(max = 200) String nome,
        @NotBlank @Email @Size(max = 200) String email,
        @NotEmpty Set<Papel> papeis,
        Long departamentoId,
        @Size(max = 20) String numeroConselho,
        @Size(max = 2) String ufConselho,
        @Size(max = 100) String especialidade) {
}
