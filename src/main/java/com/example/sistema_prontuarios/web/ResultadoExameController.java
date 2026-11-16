package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.service.RegistroClinicoService;
import com.example.sistema_prontuarios.web.dto.ResultadoExameDto;
import com.example.sistema_prontuarios.web.dto.ResultadoExameRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.example.sistema_prontuarios.audit.Auditar;

/** Lancamento do resultado de um exame requisitado (medico ou enfermeiro da instituicao). */
@RestController
@RequestMapping("/api/instituicoes/{cnpj}/requisicoes-exames/{idRequisicao}/resultados")
@PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
public class ResultadoExameController {

    private final RegistroClinicoService service;

    public ResultadoExameController(RegistroClinicoService service) {
        this.service = service;
    }

    @Auditar(acao = "LANCAR_RESULTADO_EXAME", recurso = "RESULTADO_EXAME")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultadoExameDto lancar(@PathVariable String cnpj, @PathVariable Long idRequisicao,
                                    @Valid @RequestBody ResultadoExameRequest requisicao,
                                    @AuthenticationPrincipal Jwt jwt) {
        return service.lancarResultado(cnpj, idRequisicao, requisicao, Long.valueOf(jwt.getSubject()));
    }
}
