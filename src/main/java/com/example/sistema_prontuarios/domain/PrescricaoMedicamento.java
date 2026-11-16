package com.example.sistema_prontuarios.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prescricao_medicamento")
@DiscriminatorValue("PRESCRICAO_MEDICAMENTO")
@PrimaryKeyJoinColumn(name = "id_registro")
@Getter @Setter @NoArgsConstructor
public class PrescricaoMedicamento extends RegistroClinico {

    @Column(name = "medicamento", nullable = false)
    private String medicamento;

    @Column(name = "dosagem", nullable = false)
    private String dosagem;

    @Column(name = "via")
    private String via;

    @Column(name = "frequencia")
    private String frequencia;
}
