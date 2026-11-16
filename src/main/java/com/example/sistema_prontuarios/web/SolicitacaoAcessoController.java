package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.service.SolicitacaoAcessoService;
import com.example.sistema_prontuarios.web.dto.SolicitacaoAcessoDto;
import com.example.sistema_prontuarios.web.dto.SolicitacaoAcessoRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.example.sistema_prontuarios.audit.Auditar;

/** Solicitacoes de acesso aos dados de pacientes feitas pela recepcao da instituicao. */
@RestController
@RequestMapping("/api/instituicoes/{cnpj}/solicitacoes-acesso")
@PreAuthorize("hasRole('RECEPCIONISTA')")
public class SolicitacaoAcessoController {

    private final SolicitacaoAcessoService service;

    public SolicitacaoAcessoController(SolicitacaoAcessoService service) {
        this.service = service;
    }

    @Auditar(acao = "SOLICITAR_ACESSO", recurso = "SOLICITACAO_ACESSO")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitacaoAcessoDto solicitar(@PathVariable String cnpj,
                                          @Valid @RequestBody SolicitacaoAcessoRequest requisicao,
                                          @AuthenticationPrincipal Jwt jwt) {
        return service.solicitar(cnpj, requisicao, idDe(jwt));
    }

    @GetMapping
    public List<SolicitacaoAcessoDto> listar(@PathVariable String cnpj, @AuthenticationPrincipal Jwt jwt) {
        return service.listar(cnpj, idDe(jwt));
    }

    private static Long idDe(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
