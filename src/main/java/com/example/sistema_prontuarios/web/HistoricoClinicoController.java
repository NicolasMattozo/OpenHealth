package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.service.ProntuarioService;
import com.example.sistema_prontuarios.web.dto.AtendimentoHistoricoDto;
import com.example.sistema_prontuarios.web.dto.RegistroClinicoDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.sistema_prontuarios.audit.Auditar;

/** Consultas do profissional sobre um paciente (casos de uso 10, 12, 13 e 14). */
@RestController
@RequestMapping("/api/instituicoes/{cnpj}/pacientes/{cpf}")
@PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
public class HistoricoClinicoController {

    private final ProntuarioService service;

    public HistoricoClinicoController(ProntuarioService service) {
        this.service = service;
    }

    @Auditar(acao = "CONSULTAR_HISTORICO", recurso = "PACIENTE")
    @GetMapping("/historico")
    public List<AtendimentoHistoricoDto> historico(@PathVariable String cnpj, @PathVariable String cpf,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return service.historico(cnpj, cpf, idDe(jwt));
    }

    @Auditar(acao = "CONSULTAR_MEDICAMENTOS", recurso = "PACIENTE")
    @GetMapping("/medicamentos")
    @PreAuthorize("hasRole('ENFERMEIRO')")
    public List<RegistroClinicoDto> medicamentos(@PathVariable String cnpj, @PathVariable String cpf,
                                                 @AuthenticationPrincipal Jwt jwt) {
        return service.medicamentos(cnpj, cpf, idDe(jwt));
    }

    @Auditar(acao = "CONSULTAR_REQUISICOES", recurso = "PACIENTE")
    @GetMapping("/requisicoes-exames")
    public List<RegistroClinicoDto> requisicoes(@PathVariable String cnpj, @PathVariable String cpf,
                                                @AuthenticationPrincipal Jwt jwt) {
        return service.requisicoes(cnpj, cpf, idDe(jwt));
    }

    @Auditar(acao = "CONSULTAR_EXAMES", recurso = "PACIENTE")
    @GetMapping("/exames")
    @PreAuthorize("hasRole('MEDICO')")
    public List<RegistroClinicoDto> exames(@PathVariable String cnpj, @PathVariable String cpf,
                                           @AuthenticationPrincipal Jwt jwt) {
        return service.exames(cnpj, cpf, idDe(jwt));
    }

    private static Long idDe(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
