package com.example.sistema_prontuarios.domain;

import com.example.sistema_prontuarios.domain.enums.Papel;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class UsuarioPapelId implements Serializable {

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "cnpj_instituicao", length = 14)
    private String cnpjInstituicao;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", length = 30)
    private Papel papel;
}
