package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.PrescricaoMedicamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrescricaoMedicamentoRepository extends JpaRepository<PrescricaoMedicamento, Long> {

    @Query("select p from PrescricaoMedicamento p "
         + "where p.atendimento.prontuario.cpfPaciente = :cpf order by p.dataRegistro desc")
    List<PrescricaoMedicamento> listarPorPaciente(@Param("cpf") String cpf);
}
