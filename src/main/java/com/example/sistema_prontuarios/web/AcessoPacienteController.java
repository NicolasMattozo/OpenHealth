package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.service.AcessoPacienteService;
import com.example.sistema_prontuarios.web.dto.SolicitacaoParaPacienteDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.sistema_prontuarios.audit.Auditar;

/** Area do paciente: decidir quem acessa os seus dados. */
@RestController
@RequestMapping("/api/paciente/solicitacoes-acesso")
@PreAuthorize("hasRole('PACIENTE')")
public class AcessoPacienteController {

    private final AcessoPacienteService service;

    public AcessoPacienteController(AcessoPacienteService service) {
        this.service = service;
    }

    @GetMapping
    public List<SolicitacaoParaPacienteDto> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(idDe(jwt));
    }

    @Auditar(acao = "APROVAR_ACESSO", recurso = "SOLICITACAO_ACESSO")
    @PostMapping("/{id}/aprovar")
    public SolicitacaoParaPacienteDto aprovar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.aprovar(idDe(jwt), id);
    }

    @Auditar(acao = "NEGAR_ACESSO", recurso = "SOLICITACAO_ACESSO")
    @PostMapping("/{id}/negar")
    public SolicitacaoParaPacienteDto negar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.negar(idDe(jwt), id);
    }

    @Auditar(acao = "REVOGAR_ACESSO", recurso = "SOLICITACAO_ACESSO")
    @PostMapping("/{id}/revogar")
    public SolicitacaoParaPacienteDto revogar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.revogar(idDe(jwt), id);
    }

    private static Long idDe(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
