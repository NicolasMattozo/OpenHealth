package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.ProfissionalSaude;
import com.example.sistema_prontuarios.domain.ProfissionalSaudeId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfissionalSaudeRepository extends JpaRepository<ProfissionalSaude, ProfissionalSaudeId> {

    @Query("select p from ProfissionalSaude p where p.id.idUsuario = :idUsuario")
    List<ProfissionalSaude> listarPorUsuario(@Param("idUsuario") Long idUsuario);

    @Query("select p from ProfissionalSaude p where p.id.idUsuario in :ids")
    List<ProfissionalSaude> listarPorUsuarios(@Param("ids") Collection<Long> ids);
}
