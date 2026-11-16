package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.Instituicao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstituicaoRepository extends JpaRepository<Instituicao, String> {
}
