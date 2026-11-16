package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.service.ProntuarioService;
import com.example.sistema_prontuarios.web.dto.AtendimentoHistoricoDto;
import com.example.sistema_prontuarios.web.dto.ProntuarioDetalhadoDto;
import com.example.sistema_prontuarios.web.dto.RegistroClinicoDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** O paciente consulta o proprio prontuario (somente o que foi marcado como visivel a ele). */
@RestController
@RequestMapping("/api/paciente")
@PreAuthorize("hasRole('PACIENTE')")
public class ProntuarioPacienteController {

    private final ProntuarioService service;

    public ProntuarioPacienteController(ProntuarioService service) {
        this.service = service;
    }

    @GetMapping("/historico")
    public List<AtendimentoHistoricoDto> historico(@AuthenticationPrincipal Jwt jwt) {
        return service.historicoDoPaciente(idDe(jwt));
    }

    @GetMapping("/atendimentos/{idAtendimento}")
    public ProntuarioDetalhadoDto prontuario(@PathVariable Long idAtendimento, @AuthenticationPrincipal Jwt jwt) {
        return service.prontuarioDoPaciente(idDe(jwt), idAtendimento);
    }

    @GetMapping("/medicamentos")
    public List<RegistroClinicoDto> medicamentos(@AuthenticationPrincipal Jwt jwt) {
        return service.medicamentosDoPaciente(idDe(jwt));
    }

    @GetMapping("/requisicoes-exames")
    public List<RegistroClinicoDto> requisicoes(@AuthenticationPrincipal Jwt jwt) {
        return service.requisicoesDoPaciente(idDe(jwt));
    }

    @GetMapping("/exames")
    public List<RegistroClinicoDto> exames(@AuthenticationPrincipal Jwt jwt) {
        return service.examesDoPaciente(idDe(jwt));
    }

    private static Long idDe(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
