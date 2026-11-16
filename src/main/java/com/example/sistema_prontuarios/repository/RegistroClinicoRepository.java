package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.RegistroClinico;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroClinicoRepository extends JpaRepository<RegistroClinico, Long> {

    /** Linha do tempo do atendimento, do registro mais antigo para o mais recente. */
    @Query("select r from RegistroClinico r where r.atendimento.id = :idAtendimento order by r.dataRegistro, r.id")
    List<RegistroClinico> listarPorAtendimento(@Param("idAtendimento") Long idAtendimento);
}
