package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.service.PacienteJaCadastradoException;
import com.example.sistema_prontuarios.web.dto.PacienteDto;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Todo erro da API sai no formato {"erro": "mensagem", "campos": {...}} para o frontend exibir. */
@RestControllerAdvice
public class ApiExceptionHandler {

    public record ErroDto(String erro, Map<String, String> campos) {
    }

    /** Resposta do cadastro de paciente com CPF repetido: traz os dados ja cadastrados. */
    public record ErroPacienteDto(String erro, Map<String, String> campos, PacienteDto paciente) {
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroDto> status(ResponseStatusException ex) {
        String mensagem = ex.getReason() != null ? ex.getReason() : "Não foi possível concluir a operação.";
        return ResponseEntity.status(ex.getStatusCode()).body(new ErroDto(mensagem, Map.of()));
    }

    @ExceptionHandler(PacienteJaCadastradoException.class)
    public ResponseEntity<ErroPacienteDto> pacienteJaCadastrado(PacienteJaCadastradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErroPacienteDto("Já existe um paciente cadastrado com este CPF.", Map.of(), ex.getPaciente()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroDto> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErroDto("Dados inválidos.", campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroDto> corpoIlegivel(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErroDto("Corpo da requisição inválido.", Map.of()));
    }

    /** Ex.: dois cadastros simultaneos com o mesmo e-mail. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroDto> integridade(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroDto("Os dados conflitam com um registro já existente.", Map.of()));
    }
}
