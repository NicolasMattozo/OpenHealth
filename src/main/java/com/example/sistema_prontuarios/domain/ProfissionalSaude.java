package com.example.sistema_prontuarios.domain;

import com.example.sistema_prontuarios.domain.enums.TipoProfissional;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profissional_saude")
@Getter @Setter @NoArgsConstructor
public class ProfissionalSaude {

    @EmbeddedId
    private ProfissionalSaudeId id = new ProfissionalSaudeId();

    @MapsId("idUsuario")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "numero_conselho", nullable = false)
    private String numeroConselho;

    @Column(name = "uf_conselho", nullable = false, length = 2)
    private String ufConselho;

    @Column(name = "especialidade")
    private String especialidade;

    public TipoProfissional getTipo() {
        return id.getTipo();
    }
}
