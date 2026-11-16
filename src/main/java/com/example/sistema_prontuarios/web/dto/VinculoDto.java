package com.example.sistema_prontuarios.web.dto;

import java.util.List;

/** Instituicao em que o usuario atua e os papeis que exerce nela. */
public record VinculoDto(String cnpj, String nomeInstituicao, List<String> papeis) {
}
