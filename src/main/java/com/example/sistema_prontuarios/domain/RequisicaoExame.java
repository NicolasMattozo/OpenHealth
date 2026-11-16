package com.example.sistema_prontuarios.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "requisicao_exame")
@DiscriminatorValue("REQUISICAO_EXAME")
@PrimaryKeyJoinColumn(name = "id_registro")
@Getter @Setter @NoArgsConstructor
public class RequisicaoExame extends RegistroClinico {

    @Column(name = "tipo_exame", nullable = false)
    private String tipoExame;
}
