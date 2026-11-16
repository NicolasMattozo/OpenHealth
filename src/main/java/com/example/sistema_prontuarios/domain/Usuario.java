package com.example.sistema_prontuarios.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter @Setter @NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "nome", nullable = false)
    private String nome;

    /** Nulo enquanto o usuario (ex.: paciente cadastrado pela recepcao) nao definiu a senha. */
    @Column(name = "senha_hash")
    private String senhaHash;

    @Column(name = "administrador_sistema", nullable = false)
    private boolean administradorSistema = false;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    /** Verdadeiro enquanto o usuario usa a senha temporaria entregue no cadastro. */
    @Column(name = "deve_trocar_senha", nullable = false)
    private boolean deveTrocarSenha = false;
}
