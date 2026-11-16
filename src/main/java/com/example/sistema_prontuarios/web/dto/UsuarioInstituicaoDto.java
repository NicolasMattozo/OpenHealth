package com.example.sistema_prontuarios.web.dto;

import java.util.List;

/** Usuario visto dentro de uma instituicao. "ativo" e o estado do vinculo com ela. */
public record UsuarioInstituicaoDto(
        Long id,
        String nome,
        String email,
        boolean ativo,
        List<String> papeis,
        Long departamentoId,
        String departamentoNome,
        String numeroConselho,
        String ufConselho,
        String especialidade) {
}
