package com.example.sistema_prontuarios.web.dto;

import com.example.sistema_prontuarios.domain.enums.Papel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** Substitui os papeis e o departamento do usuario na instituicao. */
public record UsuarioPapeisRequest(
        @NotEmpty Set<Papel> papeis,
        Long departamentoId,
        @Size(max = 20) String numeroConselho,
        @Size(max = 2) String ufConselho,
        @Size(max = 100) String especialidade) {
}
