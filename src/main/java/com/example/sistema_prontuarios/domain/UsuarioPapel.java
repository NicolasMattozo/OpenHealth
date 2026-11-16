package com.example.sistema_prontuarios.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Um papel que o usuario exerce em uma instituicao (a chave composta e todo o registro). */
@Entity
@Table(name = "usuario_papel")
@Getter @Setter @NoArgsConstructor
public class UsuarioPapel {

    @EmbeddedId
    private UsuarioPapelId id;

    public UsuarioPapel(UsuarioPapelId id) {
        this.id = id;
    }
}
