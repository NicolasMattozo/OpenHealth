package com.example.sistema_prontuarios.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dado_clinico")
@DiscriminatorValue("DADO_CLINICO")
@PrimaryKeyJoinColumn(name = "id_registro")
@Getter @Setter @NoArgsConstructor
public class DadoClinico extends RegistroClinico {

    /** Tipo do dado (ex.: pressao arterial, peso). Coluna "tipo" da tabela dado_clinico. */
    @Column(name = "tipo", nullable = false)
    private String tipoDado;

    @Column(name = "valor", nullable = false)
    private String valor;
}
