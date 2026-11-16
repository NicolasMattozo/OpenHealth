package com.example.sistema_prontuarios.domain;

import com.example.sistema_prontuarios.domain.enums.TipoProfissional;
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
public class ProfissionalSaudeId implements Serializable {

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20)
    private TipoProfissional tipo;
}
