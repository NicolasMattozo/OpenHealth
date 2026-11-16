package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Atendimento;
import com.example.sistema_prontuarios.domain.ProfissionalSaude;
import com.example.sistema_prontuarios.domain.enums.Papel;
import com.example.sistema_prontuarios.domain.enums.StatusAtendimento;
import com.example.sistema_prontuarios.domain.enums.TipoProfissional;
import com.example.sistema_prontuarios.util.Cnpj;
import com.example.sistema_prontuarios.web.dto.AtendimentoDto;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** O medico assume (inicia) e encerra o atendimento aberto pela recepcao. */
@Service
public class AtendimentoClinicoService {

    private final ClinicaSupport apoio;
    private final PermissaoService permissao;

    public AtendimentoClinicoService(ClinicaSupport apoio, PermissaoService permissao) {
        this.apoio = apoio;
        this.permissao = permissao;
    }

    /** ABERTO -> EM_ANDAMENTO, com este medico como responsavel. Repetir a chamada do mesmo medico nao muda nada. */
    @Transactional
    public AtendimentoDto iniciar(String cnpjParam, Long idAtendimento, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirPapel(solicitante, cnpj, Papel.MEDICO);
        Atendimento atendimento = apoio.atendimentoDaInstituicao(idAtendimento, cnpj);
        ProfissionalSaude medico = apoio.profissional(solicitante, TipoProfissional.MEDICO);

        boolean livre = atendimento.getStatus() == StatusAtendimento.ABERTO && atendimento.getMedico() == null;
        boolean jaEDele = atendimento.getStatus() == StatusAtendimento.EM_ANDAMENTO
                && atendimento.getMedico() != null
                && atendimento.getMedico().getId().equals(medico.getId());
        if (livre) {
            atendimento.setMedico(medico);
            atendimento.setStatus(StatusAtendimento.EM_ANDAMENTO);
        } else if (!jaEDele) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este atendimento não pode ser iniciado: já foi assumido por outro médico ou foi encerrado.");
        }
        return AtendimentoService.paraDto(atendimento);
    }

    /** EM_ANDAMENTO -> FINALIZADO; so o medico responsavel. */
    @Transactional
    public AtendimentoDto finalizar(String cnpjParam, Long idAtendimento, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirPapel(solicitante, cnpj, Papel.MEDICO);
        Atendimento atendimento = apoio.atendimentoDaInstituicao(idAtendimento, cnpj);
        apoio.medicoResponsavel(solicitante, cnpj, atendimento);

        atendimento.setStatus(StatusAtendimento.FINALIZADO);
        atendimento.setDataFinalizacao(Instant.now());
        return AtendimentoService.paraDto(atendimento);
    }
}
