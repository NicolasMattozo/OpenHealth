package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.service.AtendimentoClinicoService;
import com.example.sistema_prontuarios.service.ProntuarioService;
import com.example.sistema_prontuarios.service.RegistroClinicoService;
import com.example.sistema_prontuarios.web.dto.AtendimentoDto;
import com.example.sistema_prontuarios.web.dto.DadoClinicoRequest;
import com.example.sistema_prontuarios.web.dto.DiagnosticoRequest;
import com.example.sistema_prontuarios.web.dto.PrescricaoRequest;
import com.example.sistema_prontuarios.web.dto.ProntuarioDetalhadoDto;
import com.example.sistema_prontuarios.web.dto.RegistroClinicoDto;
import com.example.sistema_prontuarios.web.dto.RequisicaoExameRequest;
import com.example.sistema_prontuarios.web.dto.TriagemRequest;
import jakarta.validation.Valid;
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

/** O que acontece dentro de um atendimento: o medico assume e encerra; medico e enfermeiro registram. */
@RestController
@RequestMapping("/api/instituicoes/{cnpj}/atendimentos/{idAtendimento}")
@PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
public class AtendimentoClinicoController {

    private final AtendimentoClinicoService atendimentoService;
    private final RegistroClinicoService registroService;
    private final ProntuarioService prontuarioService;

    public AtendimentoClinicoController(AtendimentoClinicoService atendimentoService,
                                        RegistroClinicoService registroService,
                                        ProntuarioService prontuarioService) {
        this.atendimentoService = atendimentoService;
        this.registroService = registroService;
        this.prontuarioService = prontuarioService;
    }

    @Auditar(acao = "INICIAR_ATENDIMENTO", recurso = "ATENDIMENTO")
    @PostMapping("/iniciar")
    @PreAuthorize("hasRole('MEDICO')")
    public AtendimentoDto iniciar(@PathVariable String cnpj, @PathVariable Long idAtendimento,
                                  @AuthenticationPrincipal Jwt jwt) {
        return atendimentoService.iniciar(cnpj, idAtendimento, idDe(jwt));
    }

    @Auditar(acao = "FINALIZAR_ATENDIMENTO", recurso = "ATENDIMENTO")
    @PostMapping("/finalizar")
    @PreAuthorize("hasRole('MEDICO')")
    public AtendimentoDto finalizar(@PathVariable String cnpj, @PathVariable Long idAtendimento,
                                    @AuthenticationPrincipal Jwt jwt) {
        return atendimentoService.finalizar(cnpj, idAtendimento, idDe(jwt));
    }

    @Auditar(acao = "REGISTRAR_TRIAGEM", recurso = "REGISTRO_CLINICO")
    @PostMapping("/triagem")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ENFERMEIRO')")
    public RegistroClinicoDto triagem(@PathVariable String cnpj, @PathVariable Long idAtendimento,
                                      @Valid @RequestBody TriagemRequest requisicao, @AuthenticationPrincipal Jwt jwt) {
        return registroService.registrarTriagem(cnpj, idAtendimento, requisicao, idDe(jwt));
    }

    @Auditar(acao = "REGISTRAR_DADO_CLINICO", recurso = "REGISTRO_CLINICO")
    @PostMapping("/dados-clinicos")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroClinicoDto dadoClinico(@PathVariable String cnpj, @PathVariable Long idAtendimento,
                                          @Valid @RequestBody DadoClinicoRequest requisicao,
                                          @AuthenticationPrincipal Jwt jwt) {
        return registroService.registrarDadoClinico(cnpj, idAtendimento, requisicao, idDe(jwt));
    }

    @Auditar(acao = "REGISTRAR_DIAGNOSTICO", recurso = "REGISTRO_CLINICO")
    @PostMapping("/diagnosticos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEDICO')")
    public RegistroClinicoDto diagnostico(@PathVariable String cnpj, @PathVariable Long idAtendimento,
                                          @Valid @RequestBody DiagnosticoRequest requisicao,
                                          @AuthenticationPrincipal Jwt jwt) {
        return registroService.registrarDiagnostico(cnpj, idAtendimento, requisicao, idDe(jwt));
    }

    @Auditar(acao = "REGISTRAR_PRESCRICAO", recurso = "REGISTRO_CLINICO")
    @PostMapping("/prescricoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEDICO')")
    public RegistroClinicoDto prescricao(@PathVariable String cnpj, @PathVariable Long idAtendimento,
                                         @Valid @RequestBody PrescricaoRequest requisicao,
                                         @AuthenticationPrincipal Jwt jwt) {
        return registroService.registrarPrescricao(cnpj, idAtendimento, requisicao, idDe(jwt));
    }

    @Auditar(acao = "REGISTRAR_REQUISICAO_EXAME", recurso = "REGISTRO_CLINICO")
    @PostMapping("/requisicoes-exames")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEDICO')")
    public RegistroClinicoDto requisicaoExame(@PathVariable String cnpj, @PathVariable Long idAtendimento,
                                              @Valid @RequestBody RequisicaoExameRequest requisicao,
                                              @AuthenticationPrincipal Jwt jwt) {
        return registroService.registrarRequisicaoExame(cnpj, idAtendimento, requisicao, idDe(jwt));
    }

    /** Prontuario detalhado do atendimento (caso de uso 11). */
    @Auditar(acao = "CONSULTAR_PRONTUARIO", recurso = "ATENDIMENTO")
    @GetMapping("/prontuario")
    public ProntuarioDetalhadoDto prontuario(@PathVariable String cnpj, @PathVariable Long idAtendimento,
                                             @AuthenticationPrincipal Jwt jwt) {
        return prontuarioService.prontuarioDetalhado(cnpj, idAtendimento, idDe(jwt));
    }

    private static Long idDe(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
