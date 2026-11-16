package com.example.sistema_prontuarios.web.dto;

import java.util.List;

/** Enquanto "deveTrocarSenha" for verdadeiro, roles e instituicoes vem vazios. */
public record UsuarioLogadoDto(
        Long id,
        String nome,
        String email,
        String cpf,
        List<String> roles,
        List<VinculoDto> instituicoes,
        boolean deveTrocarSenha) {
}
