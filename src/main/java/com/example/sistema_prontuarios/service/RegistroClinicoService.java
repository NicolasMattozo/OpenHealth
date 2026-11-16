package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Atendimento;
import com.example.sistema_prontuarios.domain.CondicaoDiagnostico;
import com.example.sistema_prontuarios.domain.DadoClinico;
import com.example.sistema_prontuarios.domain.PrescricaoMedicamento;
import com.example.sistema_prontuarios.domain.ProfissionalSaude;
import com.example.sistema_prontuarios.domain.RegistroClinico;
import com.example.sistema_prontuarios.domain.RequisicaoExame;
import com.example.sistema_prontuarios.domain.ResultadoExame;
import com.example.sistema_prontuarios.domain.ResultadoExameId;
import com.example.sistema_prontuarios.domain.Triagem;
import com.example.sistema_prontuarios.domain.enums.Papel;
import com.example.sistema_prontuarios.domain.enums.StatusAtendimento;
import com.example.sistema_prontuarios.domain.enums.TipoProfissional;
import com.example.sistema_prontuarios.repository.RequisicaoExameRepository;
import com.example.sistema_prontuarios.repository.ResultadoExameRepository;
import com.example.sistema_prontuarios.util.Cnpj;
import com.example.sistema_prontuarios.web.dto.DadoClinicoRequest;
import com.example.sistema_prontuarios.web.dto.DiagnosticoRequest;
import com.example.sistema_prontuarios.web.dto.PrescricaoRequest;
import com.example.sistema_prontuarios.web.dto.RegistroClinicoDto;
import com.example.sistema_prontuarios.web.dto.RequisicaoExameRequest;
import com.example.sistema_prontuarios.web.dto.ResultadoExameDto;
import com.example.sistema_prontuarios.web.dto.ResultadoExameRequest;
import com.example.sistema_prontuarios.web.dto.TriagemRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.sistema_prontuarios.audit.AuditoriaContexto;

/**
 * Casos de uso 5 a 9: registrar triagem, dados clinicos, diagnostico, prescricao e requisicao de exame,
 * e lancar o resultado do exame. Cada registro fica ligado a um atendimento da propria instituicao.
 */
@Service
public class RegistroClinicoService {

    private final ClinicaSupport apoio;
    private final PermissaoService permissao;
    private final RequisicaoExameRepository requisicoes;
    private final ResultadoExameRepository resultados;

    @PersistenceContext
    private EntityManager em;

    public RegistroClinicoService(ClinicaSupport apoio,
                           PermissaoService permissao,
                           RequisicaoExameRepository requisicoes,
                           ResultadoExameRepository resultados) {
        this.apoio = apoio;
        this.permissao = permissao;
        this.requisicoes = requisicoes;
        this.resultados = resultados;
    }

    /** Caso de uso 5 (enfermeiro). */
    @Transactional
    public RegistroClinicoDto registrarTriagem(String cnpjParam, Long idAtendimento,
                                               TriagemRequest requisicao, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirPapel(solicitante, cnpj, Papel.ENFERMEIRO);
        Atendimento atendimento = apoio.atendimentoDaInstituicao(idAtendimento, cnpj);
        apoio.exigirAberto(atendimento);
        ProfissionalSaude autor = apoio.profissional(solicitante, TipoProfissional.ENFERMEIRO);

        Triagem triagem = new Triagem();
        preencherBase(triagem, atendimento, autor, requisicao.visivelAoPaciente());
        triagem.setClassificacaoRisco(requisicao.classificacaoRisco());
        triagem.setSintomas(vazioParaNulo(requisicao.sintomas()));
        em.persist(triagem);
        AuditoriaContexto.adicionar("idRegistro", triagem.getId());
        return RegistroMapper.paraDto(triagem, List.of());
    }

    /** Caso de uso 7 (medico ou enfermeiro). */
    @Transactional
    public RegistroClinicoDto registrarDadoClinico(String cnpjParam, Long idAtendimento,
                                                   DadoClinicoRequest requisicao, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        TipoProfissional tipo = permissao.exigirProfissional(solicitante, cnpj);
        Atendimento atendimento = apoio.atendimentoDaInstituicao(idAtendimento, cnpj);
        ProfissionalSaude autor;
        if (tipo == TipoProfissional.MEDICO) {
            autor = apoio.medicoResponsavel(solicitante, cnpj, atendimento);
        } else {
            apoio.exigirAberto(atendimento);
            autor = apoio.profissional(solicitante, TipoProfissional.ENFERMEIRO);
        }

        DadoClinico dado = new DadoClinico();
        preencherBase(dado, atendimento, autor, requisicao.visivelAoPaciente());
        dado.setTipoDado(requisicao.tipo().trim());
        dado.setValor(requisicao.valor().trim());
        em.persist(dado);
        AuditoriaContexto.adicionar("idRegistro", dado.getId());
        return RegistroMapper.paraDto(dado, List.of());
    }

    /** Caso de uso 6 (medico responsavel). */
    @Transactional
    public RegistroClinicoDto registrarDiagnostico(String cnpjParam, Long idAtendimento,
                                                   DiagnosticoRequest requisicao, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        Atendimento atendimento = apoio.atendimentoDaInstituicao(idAtendimento, cnpj);
        ProfissionalSaude autor = apoio.medicoResponsavel(solicitante, cnpj, atendimento);

        CondicaoDiagnostico diagnostico = new CondicaoDiagnostico();
        preencherBase(diagnostico, atendimento, autor, requisicao.visivelAoPaciente());
        diagnostico.setDescricao(requisicao.descricao().trim());
        diagnostico.setCid(vazioParaNulo(requisicao.cid()) == null ? null : requisicao.cid().trim().toUpperCase());
        em.persist(diagnostico);
        AuditoriaContexto.adicionar("idRegistro", diagnostico.getId());
        return RegistroMapper.paraDto(diagnostico, List.of());
    }

    /** Caso de uso 8 (medico responsavel). */
    @Transactional
    public RegistroClinicoDto registrarPrescricao(String cnpjParam, Long idAtendimento,
                                                  PrescricaoRequest requisicao, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        Atendimento atendimento = apoio.atendimentoDaInstituicao(idAtendimento, cnpj);
        ProfissionalSaude autor = apoio.medicoResponsavel(solicitante, cnpj, atendimento);

        PrescricaoMedicamento prescricao = new PrescricaoMedicamento();
        preencherBase(prescricao, atendimento, autor, requisicao.visivelAoPaciente());
        prescricao.setMedicamento(requisicao.medicamento().trim());
        prescricao.setDosagem(requisicao.dosagem().trim());
        prescricao.setVia(vazioParaNulo(requisicao.via()));
        prescricao.setFrequencia(vazioParaNulo(requisicao.frequencia()));
        em.persist(prescricao);
        AuditoriaContexto.adicionar("idRegistro", prescricao.getId());
        return RegistroMapper.paraDto(prescricao, List.of());
    }

    /** Caso de uso 9 (medico responsavel). */
    @Transactional
    public RegistroClinicoDto registrarRequisicaoExame(String cnpjParam, Long idAtendimento,
                                                       RequisicaoExameRequest requisicao, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        Atendimento atendimento = apoio.atendimentoDaInstituicao(idAtendimento, cnpj);
        ProfissionalSaude autor = apoio.medicoResponsavel(solicitante, cnpj, atendimento);

        RequisicaoExame exame = new RequisicaoExame();
        preencherBase(exame, atendimento, autor, requisicao.visivelAoPaciente());
        exame.setTipoExame(requisicao.tipoExame().trim());
        em.persist(exame);
        AuditoriaContexto.adicionar("idRegistro", exame.getId());
        return RegistroMapper.paraDto(exame, List.of());
    }

    /**
     * Lanca o resultado de um exame requisitado em atendimento da instituicao (medico ou enfermeiro).
     * Pode ser feito depois que o atendimento foi finalizado, porque exames demoram.
     * Lancar de novo cria uma nova versao em vez de sobrescrever.
     */
    @Transactional
    public ResultadoExameDto lancarResultado(String cnpjParam, Long idRequisicao,
                                             ResultadoExameRequest requisicao, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        TipoProfissional tipo = permissao.exigirProfissional(solicitante, cnpj);
        RequisicaoExame exame = requisicoes.findById(idRequisicao)
                .filter(e -> e.getAtendimento().getInstituicao().getCnpj().equals(cnpj))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição de exame não encontrada."));
        if (exame.getAtendimento().getStatus() == StatusAtendimento.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O atendimento foi cancelado.");
        }
        ProfissionalSaude registrador = apoio.profissional(solicitante, tipo);

        Integer ultima = resultados.ultimaVersao(idRequisicao);
        ResultadoExame resultado = new ResultadoExame();
        resultado.setId(new ResultadoExameId(idRequisicao, ultima == null ? 1 : ultima + 1));
        resultado.setRequisicao(exame);
        resultado.setResultado(requisicao.resultado().trim());
        resultado.setRegistrador(registrador);
        em.persist(resultado);
        AuditoriaContexto.adicionar("cpf", exame.getAtendimento().getProntuario().getCpfPaciente());
        AuditoriaContexto.adicionar("versao", resultado.getId().getVersao());
        return RegistroMapper.resultadoParaDto(resultado);
    }

    /** Sem informar, o registro fica visivel ao paciente (e o padrao do modelo: restritoAoPaciente = false). */
    private static void preencherBase(RegistroClinico registro, Atendimento atendimento,
                                      ProfissionalSaude autor, Boolean visivelAoPaciente) {
        registro.setAtendimento(atendimento);
        registro.setAutor(autor);
        registro.setVisivelAoPaciente(visivelAoPaciente == null || visivelAoPaciente);
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
