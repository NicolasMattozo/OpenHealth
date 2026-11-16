package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.service.InstituicaoService;
import com.example.sistema_prontuarios.web.dto.InstituicaoDto;
import com.example.sistema_prontuarios.web.dto.InstituicaoRequest;
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

/** Gerenciar instituicoes: somente o administrador do sistema. */
@RestController
@RequestMapping("/api/instituicoes")
@PreAuthorize("hasRole('ADMIN_SISTEMA')")
public class InstituicaoController {

    private final InstituicaoService service;

    public InstituicaoController(InstituicaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<InstituicaoDto> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(idDe(jwt));
    }

    @GetMapping("/{cnpj}")
    public InstituicaoDto buscar(@PathVariable String cnpj, @AuthenticationPrincipal Jwt jwt) {
        return service.buscar(cnpj, idDe(jwt));
    }

    @Auditar(acao = "CADASTRAR_INSTITUICAO", recurso = "INSTITUICAO")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstituicaoDto cadastrar(@Valid @RequestBody InstituicaoRequest requisicao, @AuthenticationPrincipal Jwt jwt) {
        return service.cadastrar(requisicao, idDe(jwt));
    }

    /** Remover = inativar. */
    @Auditar(acao = "INATIVAR_INSTITUICAO", recurso = "INSTITUICAO")
    @DeleteMapping("/{cnpj}")
    public InstituicaoDto inativar(@PathVariable String cnpj, @AuthenticationPrincipal Jwt jwt) {
        return service.alterarAtivo(cnpj, false, idDe(jwt));
    }

    @Auditar(acao = "REATIVAR_INSTITUICAO", recurso = "INSTITUICAO")
    @PostMapping("/{cnpj}/reativar")
    public InstituicaoDto reativar(@PathVariable String cnpj, @AuthenticationPrincipal Jwt jwt) {
        return service.alterarAtivo(cnpj, true, idDe(jwt));
    }

    private static Long idDe(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
