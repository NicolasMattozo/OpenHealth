package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.SolicitacaoAcesso;
import com.example.sistema_prontuarios.domain.enums.StatusSolicitacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitacaoAcessoRepository extends JpaRepository<SolicitacaoAcesso, Long> {

    /** Para PENDENTE e APROVADA existe no maximo uma por paciente e instituicao (indice unico parcial). */
    @Query("select s from SolicitacaoAcesso s "
         + "where s.paciente.cpf = :cpf and s.instituicao.cnpj = :cnpj and s.status = :status")
    Optional<SolicitacaoAcesso> buscar(@Param("cpf") String cpf, @Param("cnpj") String cnpj,
                                       @Param("status") StatusSolicitacao status);

    @Query("select s from SolicitacaoAcesso s where s.instituicao.cnpj = :cnpj order by s.dataSolicitacao desc")
    List<SolicitacaoAcesso> listarPorInstituicao(@Param("cnpj") String cnpj, Pageable pageable);

    @Query("select s from SolicitacaoAcesso s where s.paciente.cpf = :cpf order by s.dataSolicitacao desc")
    List<SolicitacaoAcesso> listarPorPaciente(@Param("cpf") String cpf, Pageable pageable);
}
