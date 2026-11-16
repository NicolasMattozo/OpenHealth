package com.example.sistema_prontuarios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class UsuarioInstituicaoId implements Serializable {

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "cnpj_instituicao", length = 14)
    private String cnpjInstituicao;
}
