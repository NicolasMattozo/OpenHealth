package com.example.sistema_prontuarios.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "condicao_diagnostico")
@DiscriminatorValue("CONDICAO_DIAGNOSTICO")
@PrimaryKeyJoinColumn(name = "id_registro")
@Getter @Setter @NoArgsConstructor
public class CondicaoDiagnostico extends RegistroClinico {

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "cid")
    private String cid;
}
