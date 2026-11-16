package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.Paciente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PacienteRepository extends JpaRepository<Paciente, String> {

    @Query("select p from Paciente p where p.usuario.id = :idUsuario")
    Optional<Paciente> buscarPorUsuario(@Param("idUsuario") Long idUsuario);

    /** CPF exato ou parte do nome. O curinga do LIKE usa "!" como escape (o texto ja vem escapado). */
    @Query("select p from Paciente p join p.usuario u "
         + "where p.cpf = :cpf or lower(u.nome) like lower(concat('%', :nome, '%')) escape '!' "
         + "order by u.nome")
    List<Paciente> buscar(@Param("cpf") String cpf, @Param("nome") String nome, Pageable pageable);
}
