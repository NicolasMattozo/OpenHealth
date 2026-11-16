package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.service.DepartamentoService;
import com.example.sistema_prontuarios.web.dto.DepartamentoDto;
import com.example.sistema_prontuarios.web.dto.DepartamentoRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.example.sistema_prontuarios.audit.Auditar;

/** Listar: admin do sistema ou da instituicao. Criar e inativar: somente admin do sistema. */
@RestController
@RequestMapping("/api/instituicoes/{cnpj}/departamentos")
@PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'ADMIN_INSTITUICAO')")
public class DepartamentoController {

    private final DepartamentoService service;

    public DepartamentoController(DepartamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<DepartamentoDto> listar(@PathVariable String cnpj, @AuthenticationPrincipal Jwt jwt) {
        return service.listar(cnpj, idDe(jwt));
    }

    @Auditar(acao = "CRIAR_DEPARTAMENTO", recurso = "DEPARTAMENTO")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public DepartamentoDto criar(@PathVariable String cnpj, @Valid @RequestBody DepartamentoRequest requisicao,
                                 @AuthenticationPrincipal Jwt jwt) {
        return service.criar(cnpj, requisicao, idDe(jwt));
    }

    /** Remover = inativar. */
    @Auditar(acao = "INATIVAR_DEPARTAMENTO", recurso = "DEPARTAMENTO")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public DepartamentoDto inativar(@PathVariable String cnpj, @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.alterarAtivo(cnpj, id, false, idDe(jwt));
    }

    @Auditar(acao = "REATIVAR_DEPARTAMENTO", recurso = "DEPARTAMENTO")
    @PostMapping("/{id}/reativar")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public DepartamentoDto reativar(@PathVariable String cnpj, @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.alterarAtivo(cnpj, id, true, idDe(jwt));
    }

    private static Long idDe(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
