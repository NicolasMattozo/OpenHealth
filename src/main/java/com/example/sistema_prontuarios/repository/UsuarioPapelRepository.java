package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.UsuarioPapel;
import com.example.sistema_prontuarios.domain.UsuarioPapelId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioPapelRepository extends JpaRepository<UsuarioPapel, UsuarioPapelId> {

    @Query("select up from UsuarioPapel up where up.id.idUsuario = :idUsuario")
    List<UsuarioPapel> listarPorUsuario(@Param("idUsuario") Long idUsuario);

    @Query("select up from UsuarioPapel up where up.id.cnpjInstituicao = :cnpj")
    List<UsuarioPapel> listarPorInstituicao(@Param("cnpj") String cnpj);
}
