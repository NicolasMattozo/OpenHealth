package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.audit.Auditar;
import com.example.sistema_prontuarios.audit.AuditoriaService;
import com.example.sistema_prontuarios.web.dto.AcessoAoPacienteDto;
import com.example.sistema_prontuarios.web.dto.LogAuditoriaDto;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta da auditoria: administradores veem o log; o paciente ve quem acessou os seus dados. */
@RestController
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    /**
     * Todas as instituicoes (admin do sistema). Filtros opcionais: idUsuario, acao, cpf, cnpj,
     * de e ate (datas ISO, ex.: 2026-11-12T00:00:00Z) e limite (padrao 100, maximo 500).
     */
    @GetMapping("/api/auditoria")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    @Auditar(acao = "CONSULTAR_AUDITORIA", recurso = "AUDITORIA")
    public List<LogAuditoriaDto> consultar(@RequestParam(required = false) Long idUsuario,
                                           @RequestParam(required = false) String acao,
                                           @RequestParam(required = false) String cpf,
                                           @RequestParam(required = false) String cnpj,
                                           @RequestParam(required = false) Instant de,
                                           @RequestParam(required = false) Instant ate,
                                           @RequestParam(required = false) Integer limite,
                                           @AuthenticationPrincipal Jwt jwt) {
        return service.consultar(Long.valueOf(jwt.getSubject()), cnpj, idUsuario, acao, cpf, de, ate, limite);
    }

    /** So os eventos daquela instituicao (admin do sistema ou admin da instituicao). */
    @GetMapping("/api/instituicoes/{cnpj}/auditoria")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'ADMIN_INSTITUICAO')")
    @Auditar(acao = "CONSULTAR_AUDITORIA", recurso = "AUDITORIA")
    public List<LogAuditoriaDto> consultarDaInstituicao(@PathVariable String cnpj,
                                                        @RequestParam(required = false) Long idUsuario,
                                                        @RequestParam(required = false) String acao,
                                                        @RequestParam(required = false) String cpf,
                                                        @RequestParam(required = false) Instant de,
                                                        @RequestParam(required = false) Instant ate,
                                                        @RequestParam(required = false) Integer limite,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return service.consultar(Long.valueOf(jwt.getSubject()), cnpj, idUsuario, acao, cpf, de, ate, limite);
    }

    /** Quem acessou ou alterou os meus dados (somente o proprio paciente). */
    @GetMapping("/api/paciente/acessos")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<AcessoAoPacienteDto> acessosAoMeusDados(@AuthenticationPrincipal Jwt jwt) {
        return service.acessosAoPaciente(Long.valueOf(jwt.getSubject()));
    }
}
