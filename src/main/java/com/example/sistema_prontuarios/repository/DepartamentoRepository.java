package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.Departamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {

    @Query("select d from Departamento d where d.instituicao.cnpj = :cnpj order by d.nome")
    List<Departamento> listarPorInstituicao(@Param("cnpj") String cnpj);

    @Query("select count(d) from Departamento d where d.instituicao.cnpj = :cnpj and lower(d.nome) = lower(:nome)")
    long contarPorNome(@Param("cnpj") String cnpj, @Param("nome") String nome);
}
