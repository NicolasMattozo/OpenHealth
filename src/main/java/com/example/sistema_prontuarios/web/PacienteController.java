package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.service.PacienteService;
import com.example.sistema_prontuarios.web.dto.PacienteAtualizacaoRequest;
import com.example.sistema_prontuarios.web.dto.PacienteCriadoDto;
import com.example.sistema_prontuarios.web.dto.PacienteDto;
import com.example.sistema_prontuarios.web.dto.PacienteRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.example.sistema_prontuarios.audit.Auditar;

/** Cadastro de pacientes pela recepcao da instituicao. */
@RestController
@RequestMapping("/api/instituicoes/{cnpj}/pacientes")
@PreAuthorize("hasRole('RECEPCIONISTA')")
public class PacienteController {

    private final PacienteService service;

    public PacienteController(PacienteService service) {
        this.service = service;
    }

    @Auditar(acao = "CADASTRAR_PACIENTE", recurso = "PACIENTE")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteCriadoDto cadastrar(@PathVariable String cnpj, @Valid @RequestBody PacienteRequest requisicao,
                                       @AuthenticationPrincipal Jwt jwt) {
        return service.cadastrar(cnpj, requisicao, idDe(jwt));
    }

    /** Busca por CPF ou parte do nome: GET ...?busca=maria */
    @Auditar(acao = "BUSCAR_PACIENTE", recurso = "PACIENTE")
    @GetMapping
    public List<PacienteDto> buscar(@PathVariable String cnpj, @RequestParam(required = false) String busca,
                                    @AuthenticationPrincipal Jwt jwt) {
        return service.buscar(cnpj, busca, idDe(jwt));
    }

    @Auditar(acao = "CONSULTAR_CADASTRO", recurso = "PACIENTE")
    @GetMapping("/{cpf}")
    public PacienteDto consultar(@PathVariable String cnpj, @PathVariable String cpf,
                                 @AuthenticationPrincipal Jwt jwt) {
        return service.consultar(cnpj, cpf, idDe(jwt));
    }

    @Auditar(acao = "ATUALIZAR_CADASTRO", recurso = "PACIENTE")
    @PutMapping("/{cpf}")
    public PacienteDto atualizarContato(@PathVariable String cnpj, @PathVariable String cpf,
                                        @Valid @RequestBody PacienteAtualizacaoRequest requisicao,
                                        @AuthenticationPrincipal Jwt jwt) {
        return service.atualizarContato(cnpj, cpf, requisicao, idDe(jwt));
    }

    private static Long idDe(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
