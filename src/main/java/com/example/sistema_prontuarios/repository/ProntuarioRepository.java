package com.example.sistema_prontuarios.repository;

import com.example.sistema_prontuarios.domain.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProntuarioRepository extends JpaRepository<Prontuario, String> {
}
