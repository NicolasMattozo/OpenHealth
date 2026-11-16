package com.example.sistema_prontuarios.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "triagem")
@DiscriminatorValue("TRIAGEM")
@PrimaryKeyJoinColumn(name = "id_registro")
@Getter @Setter @NoArgsConstructor
public class Triagem extends RegistroClinico {

    @Column(name = "classificacao_risco", nullable = false)
    private String classificacaoRisco;

    @Column(name = "sintomas")
    private String sintomas;
}
