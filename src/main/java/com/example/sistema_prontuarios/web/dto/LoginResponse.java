package com.example.sistema_prontuarios.web.dto;

public record LoginResponse(
        String token,
        String tipo,
        long expiraEmSegundos,
        UsuarioLogadoDto usuario) {
}
