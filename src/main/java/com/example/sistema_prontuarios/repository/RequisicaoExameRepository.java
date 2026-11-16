package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.RequisicaoExame;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequisicaoExameRepository extends JpaRepository<RequisicaoExame, Long> {

    @Query("select e from RequisicaoExame e "
         + "where e.atendimento.prontuario.cpfPaciente = :cpf order by e.dataRegistro desc")
    List<RequisicaoExame> listarPorPaciente(@Param("cpf") String cpf);
}
