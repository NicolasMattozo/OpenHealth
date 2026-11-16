package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.UsuarioInstituicao;
import com.example.sistema_prontuarios.domain.UsuarioInstituicaoId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioInstituicaoRepository extends JpaRepository<UsuarioInstituicao, UsuarioInstituicaoId> {

    /** Vinculos ativos do usuario com instituicoes que tambem estao ativas. */
    @Query("select ui from UsuarioInstituicao ui "
         + "where ui.id.idUsuario = :idUsuario and ui.ativo = true and ui.instituicao.ativo = true")
    List<UsuarioInstituicao> listarAtivosPorUsuario(@Param("idUsuario") Long idUsuario);

    /** Todos os vinculos da instituicao, ativos e inativos. */
    @Query("select ui from UsuarioInstituicao ui where ui.id.cnpjInstituicao = :cnpj")
    List<UsuarioInstituicao> listarPorInstituicao(@Param("cnpj") String cnpj);
}
