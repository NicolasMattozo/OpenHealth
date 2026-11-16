package com.example.sistema_prontuarios.domain;

import com.example.sistema_prontuarios.domain.enums.StatusSolicitacao;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitacao_acesso")
@Getter @Setter @NoArgsConstructor
public class SolicitacaoAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cpf_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cnpj_instituicao", nullable = false)
    private Instituicao instituicao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_recepcionista", nullable = false)
    private Usuario recepcionista;

    @Column(name = "finalidade")
    private String finalidade;

    @Column(name = "data_solicitacao", nullable = false)
    private Instant dataSolicitacao = Instant.now();

    @Column(name = "data_decisao")
    private Instant dataDecisao;

    @Column(name = "data_revogacao")
    private Instant dataRevogacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusSolicitacao status = StatusSolicitacao.PENDENTE;
}
