package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Atendimento;
import com.example.sistema_prontuarios.domain.ProfissionalSaude;
import com.example.sistema_prontuarios.domain.ProfissionalSaudeId;
import com.example.sistema_prontuarios.domain.enums.Papel;
import com.example.sistema_prontuarios.domain.enums.StatusAtendimento;
import com.example.sistema_prontuarios.domain.enums.TipoProfissional;
import com.example.sistema_prontuarios.repository.AtendimentoRepository;
import com.example.sistema_prontuarios.repository.ProfissionalSaudeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import com.example.sistema_prontuarios.audit.AuditoriaContexto;

/** Verificacoes que os servicos clinicos repetem: atendimento da instituicao, profissional e estado. */
@Component
class ClinicaSupport {

    private final AtendimentoRepository atendimentos;
    private final ProfissionalSaudeRepository profissionais;
    private final PermissaoService permissao;

    ClinicaSupport(AtendimentoRepository atendimentos,
                   ProfissionalSaudeRepository profissionais,
                   PermissaoService permissao) {
        this.atendimentos = atendimentos;
        this.profissionais = profissionais;
        this.permissao = permissao;
    }

    /** Atendimento de outra instituicao aparece como inexistente. */
    Atendimento atendimentoDaInstituicao(Long idAtendimento, String cnpj) {
        Atendimento atendimento = atendimentos.findById(idAtendimento)
                .filter(a -> a.getInstituicao().getCnpj().equals(cnpj))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atendimento não encontrado."));
        AuditoriaContexto.adicionar("cpf", atendimento.getProntuario().getCpfPaciente());
        return atendimento;
    }

    ProfissionalSaude profissional(Long idUsuario, TipoProfissional tipo) {
        return profissionais.findById(new ProfissionalSaudeId(idUsuario, tipo))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Cadastro profissional não encontrado."));
    }

    /** Triagem e dados de enfermagem: vale enquanto o atendimento nao foi encerrado. */
    void exigirAberto(Atendimento atendimento) {
        if (atendimento.getStatus() != StatusAtendimento.ABERTO
                && atendimento.getStatus() != StatusAtendimento.EM_ANDAMENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O atendimento já foi encerrado.");
        }
    }

    /** Registros medicos: so o medico que assumiu o atendimento, e com o atendimento em andamento. */
    ProfissionalSaude medicoResponsavel(Long idUsuario, String cnpj, Atendimento atendimento) {
        permissao.exigirPapel(idUsuario, cnpj, Papel.MEDICO);
        ProfissionalSaude medico = profissional(idUsuario, TipoProfissional.MEDICO);
        if (atendimento.getStatus() != StatusAtendimento.EM_ANDAMENTO
                || atendimento.getMedico() == null
                || !atendimento.getMedico().getId().equals(medico.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O atendimento precisa estar em andamento e sob a responsabilidade deste médico. Inicie o atendimento antes.");
        }
        return medico;
    }
}
