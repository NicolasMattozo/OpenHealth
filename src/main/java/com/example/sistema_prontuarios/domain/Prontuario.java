package com.example.sistema_prontuarios.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prontuario")
@Getter @Setter @NoArgsConstructor
public class Prontuario {

    /** Mesmo valor do CPF do paciente (relacao 1 para 1 compartilhando a chave). */
    @Id
    @Column(name = "cpf_paciente", length = 11)
    private String cpfPaciente;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cpf_paciente")
    private Paciente paciente;

    @Column(name = "data_abertura", nullable = false)
    private LocalDate dataAbertura = LocalDate.now();
}
