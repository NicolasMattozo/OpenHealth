package com.example.sistema_prontuarios.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Base dos registros do prontuario. Cada subtipo tem sua propria tabela e a coluna
 * "tipo" de registro_clinico diz de qual subtipo e a linha.
 */
@Entity
@Table(name = "registro_clinico")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING, length = 30)
@Getter @Setter @NoArgsConstructor
public abstract class RegistroClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_atendimento", nullable = false)
    private Atendimento atendimento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "id_usuario_autor", referencedColumnName = "id_usuario", nullable = false),
        @JoinColumn(name = "tipo_profissional_autor", referencedColumnName = "tipo", nullable = false)
    })
    private ProfissionalSaude autor;

    @Column(name = "data_registro", nullable = false)
    private Instant dataRegistro = Instant.now();

    @Column(name = "visivel_ao_paciente", nullable = false)
    private boolean visivelAoPaciente = false;
}
