package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.ResultadoExame;
import com.example.sistema_prontuarios.domain.ResultadoExameId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResultadoExameRepository extends JpaRepository<ResultadoExame, ResultadoExameId> {

    /** Ultima versao lancada para a requisicao (nulo se ainda nao ha resultado). */
    @Query("select max(r.id.versao) from ResultadoExame r where r.id.idRequisicao = :idRequisicao")
    Integer ultimaVersao(@Param("idRequisicao") Long idRequisicao);

    @Query("select r from ResultadoExame r where r.id.idRequisicao in :ids order by r.id.idRequisicao, r.id.versao")
    List<ResultadoExame> listarPorRequisicoes(@Param("ids") Collection<Long> ids);
}
