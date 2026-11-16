package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.web.dto.PacienteDto;

/** Cadastro recusado porque o CPF ja existe; carrega os dados existentes (caso de uso 2, fluxo A1). */
public class PacienteJaCadastradoException extends RuntimeException {

    private final transient PacienteDto paciente;

    public PacienteJaCadastradoException(PacienteDto paciente) {
        super("CPF ja cadastrado");
        this.paciente = paciente;
    }

    public PacienteDto getPaciente() {
        return paciente;
    }
}
