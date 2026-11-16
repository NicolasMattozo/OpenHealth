package com.example.sistema_prontuarios.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario_instituicao")
@Getter @Setter @NoArgsConstructor
public class UsuarioInstituicao {

    @EmbeddedId
    private UsuarioInstituicaoId id = new UsuarioInstituicaoId();

    @MapsId("idUsuario")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @MapsId("cnpjInstituicao")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cnpj_instituicao")
    private Instituicao instituicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_departamento")
    private Departamento departamento;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
}
