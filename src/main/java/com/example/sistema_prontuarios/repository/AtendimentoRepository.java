package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.Atendimento;
import com.example.sistema_prontuarios.domain.enums.StatusAtendimento;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    @Query("select a from Atendimento a where a.instituicao.cnpj = :cnpj order by a.dataHora desc")
    List<Atendimento> listarRecentes(@Param("cnpj") String cnpj, Pageable pageable);

    /** Quantos atendimentos do paciente na instituicao estao em um dos dois status informados. */
    @Query("select count(a) from Atendimento a "
         + "where a.prontuario.cpfPaciente = :cpf and a.instituicao.cnpj = :cnpj "
         + "and (a.status = :status1 or a.status = :status2)")
    long contarPorStatus(@Param("cpf") String cpf, @Param("cnpj") String cnpj,
                         @Param("status1") StatusAtendimento status1,
                         @Param("status2") StatusAtendimento status2);

    @Query("select a from Atendimento a where a.prontuario.cpfPaciente = :cpf order by a.dataHora desc")
    List<Atendimento> listarPorPaciente(@Param("cpf") String cpf, Pageable pageable);
}
