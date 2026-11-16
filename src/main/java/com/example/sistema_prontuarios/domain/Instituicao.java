package com.example.sistema_prontuarios.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instituicao")
@Getter @Setter @NoArgsConstructor
public class Instituicao {

    @Id
    @Column(name = "cnpj", length = 14)
    private String cnpj;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "endereco")
    private String endereco;

    /** Ex.: Hospital, Clinica, Laboratorio. */
    @Column(name = "tipo")
    private String tipo;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
}
