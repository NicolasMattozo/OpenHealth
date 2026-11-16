package com.example.sistema_prontuarios.domain;

import com.example.sistema_prontuarios.domain.enums.StatusAtendimento;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "atendimento")
@Getter @Setter @NoArgsConstructor
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_atendimento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cpf_paciente", nullable = false)
    private Prontuario prontuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cnpj_instituicao", nullable = false)
    private Instituicao instituicao;

    /** Solicitacao que liberou o acesso aos dados (nula quando nao ha acesso entre instituicoes). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitacao")
    private SolicitacaoAcesso solicitacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_recepcionista", nullable = false)
    private Usuario recepcionista;

    /** Medico responsavel; fica nulo ate o atendimento ser assumido por um medico. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "id_usuario_medico", referencedColumnName = "id_usuario"),
        @JoinColumn(name = "tipo_medico", referencedColumnName = "tipo")
    })
    private ProfissionalSaude medico;

    @Column(name = "motivo")
    private String motivo;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora = Instant.now();

    @Column(name = "data_finalizacao")
    private Instant dataFinalizacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusAtendimento status = StatusAtendimento.ABERTO;
}
