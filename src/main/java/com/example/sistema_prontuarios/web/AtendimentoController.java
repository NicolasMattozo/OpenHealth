package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.domain.enums.StatusAtendimento;
import com.example.sistema_prontuarios.service.AtendimentoService;
import com.example.sistema_prontuarios.web.dto.AtendimentoDto;
import com.example.sistema_prontuarios.web.dto.AtendimentoRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.example.sistema_prontuarios.audit.Auditar;

/** Abrir atendimento: recepcao. Listar: recepcao, medico e enfermeiro da instituicao. */
@RestController
@RequestMapping("/api/instituicoes/{cnpj}/atendimentos")
@PreAuthorize("hasAnyRole('RECEPCIONISTA', 'MEDICO', 'ENFERMEIRO')")
public class AtendimentoController {

    private final AtendimentoService service;

    public AtendimentoController(AtendimentoService service) {
        this.service = service;
    }

    @Auditar(acao = "ABRIR_ATENDIMENTO", recurso = "ATENDIMENTO")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public AtendimentoDto abrir(@PathVariable String cnpj, @Valid @RequestBody AtendimentoRequest requisicao,
                                @AuthenticationPrincipal Jwt jwt) {
        return service.abrir(cnpj, requisicao, idDe(jwt));
    }

    /** GET ...?status=ABERTO (o filtro e opcional). */
    @GetMapping
    public List<AtendimentoDto> listar(@PathVariable String cnpj,
                                       @RequestParam(required = false) StatusAtendimento status,
                                       @AuthenticationPrincipal Jwt jwt) {
        return service.listar(cnpj, status, idDe(jwt));
    }

    @Auditar(acao = "CANCELAR_ATENDIMENTO", recurso = "ATENDIMENTO")
    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'MEDICO', 'ENFERMEIRO')")
    public AtendimentoDto cancelar(@PathVariable String cnpj, @PathVariable Long id,
                                   @AuthenticationPrincipal Jwt jwt) {
        return service.cancelar(cnpj, id, idDe(jwt));
    }

    private static Long idDe(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}