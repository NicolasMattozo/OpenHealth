package com.example.sistema_prontuarios.web.dto;

/** "senhaTemporaria" so vem preenchida quando o usuario foi criado agora, e so aparece nesta resposta. */
public record UsuarioCriadoDto(UsuarioInstituicaoDto usuario, String senhaTemporaria) {
}
