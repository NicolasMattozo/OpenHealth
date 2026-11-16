package com.example.sistema_prontuarios.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Resultado de um exame requisitado. Corrigir um resultado cria uma nova versao. */
@Entity
@Table(name = "resultado_exame")
@Getter @Setter @NoArgsConstructor
public class ResultadoExame {

    @EmbeddedId
    private ResultadoExameId id = new ResultadoExameId();

    @MapsId("idRequisicao")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_requisicao")
    private RequisicaoExame requisicao;

    @Column(name = "resultado", nullable = false)
    private String resultado;

    @Column(name = "data_resultado", nullable = false)
    private Instant dataResultado = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "id_usuario_registrador", referencedColumnName = "id_usuario", nullable = false),
        @JoinColumn(name = "tipo_profissional_registrador", referencedColumnName = "tipo", nullable = false)
    })
    private ProfissionalSaude registrador;
}
