package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Atendimento;
import com.example.sistema_prontuarios.domain.Paciente;
import com.example.sistema_prontuarios.domain.PrescricaoMedicamento;
import com.example.sistema_prontuarios.domain.RegistroClinico;
import com.example.sistema_prontuarios.domain.RequisicaoExame;
import com.example.sistema_prontuarios.domain.ResultadoExame;
import com.example.sistema_prontuarios.domain.enums.Papel;
import com.example.sistema_prontuarios.domain.enums.StatusSolicitacao;
import com.example.sistema_prontuarios.repository.AtendimentoRepository;
import com.example.sistema_prontuarios.repository.PacienteRepository;
import com.example.sistema_prontuarios.repository.PrescricaoMedicamentoRepository;
import com.example.sistema_prontuarios.repository.RegistroClinicoRepository;
import com.example.sistema_prontuarios.repository.RequisicaoExameRepository;
import com.example.sistema_prontuarios.repository.ResultadoExameRepository;
import com.example.sistema_prontuarios.repository.SolicitacaoAcessoRepository;
import com.example.sistema_prontuarios.util.Cnpj;
import com.example.sistema_prontuarios.util.Cpf;
import com.example.sistema_prontuarios.web.dto.AtendimentoHistoricoDto;
import com.example.sistema_prontuarios.web.dto.ProntuarioDetalhadoDto;
import com.example.sistema_prontuarios.web.dto.RegistroClinicoDto;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.sistema_prontuarios.audit.AuditoriaContexto;

/**
 * Casos de uso 10 a 14: consultar historico, prontuario detalhado, medicamentos, requisicoes e exames.
 *
 * Quem enxerga o que:
 *  - profissional: tudo o que foi registrado nos atendimentos da PROPRIA instituicao; os de outras instituicoes
 *    so se houver solicitacao de acesso APROVADA da sua instituicao para o paciente;
 *  - paciente: os proprios registros marcados como visiveis ao paciente, em qualquer instituicao.
 */
@Service
public class ProntuarioService {

    private final AtendimentoRepository atendimentos;
    private final RegistroClinicoRepository registros;
    private final PrescricaoMedicamentoRepository prescricoes;
    private final RequisicaoExameRepository requisicoes;
    private final ResultadoExameRepository resultados;
    private final SolicitacaoAcessoRepository solicitacoes;
    private final PacienteRepository pacientes;
    private final PermissaoService permissao;

    public ProntuarioService(AtendimentoRepository atendimentos,
                             RegistroClinicoRepository registros,
                             PrescricaoMedicamentoRepository prescricoes,
                             RequisicaoExameRepository requisicoes,
                             ResultadoExameRepository resultados,
                             SolicitacaoAcessoRepository solicitacoes,
                             PacienteRepository pacientes,
                             PermissaoService permissao) {
        this.atendimentos = atendimentos;
        this.registros = registros;
        this.prescricoes = prescricoes;
        this.requisicoes = requisicoes;
        this.resultados = resultados;
        this.solicitacoes = solicitacoes;
        this.pacientes = pacientes;
        this.permissao = permissao;
    }

    // ================================================================ profissional

    /** Caso de uso 10: os atendimentos do paciente que esta instituicao pode ver (ate 100, do mais recente). */
    @Transactional(readOnly = true)
    public List<AtendimentoHistoricoDto> historico(String cnpjParam, String cpfParam, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirProfissional(solicitante, cnpj);
        String cpf = paciente(cpfParam).getCpf();

        Predicate<Atendimento> pode = podeVer(cnpj, cpf);
        List<Atendimento> visiveis = atendimentos.listarPorPaciente(cpf, PageRequest.of(0, 100)).stream()
                .filter(pode)
                .toList();
        AuditoriaContexto.adicionar("entreInstituicoes", cruzaInstituicoes(visiveis, cnpj));
        return visiveis.stream().map(ProntuarioService::resumo).toList();
    }

    /** Caso de uso 11: todos os registros de um atendimento, em ordem cronologica. */
    @Transactional(readOnly = true)
    public ProntuarioDetalhadoDto prontuarioDetalhado(String cnpjParam, Long idAtendimento, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirProfissional(solicitante, cnpj);
        Atendimento atendimento = atendimentos.findById(idAtendimento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atendimento não encontrado."));

        String cpf = atendimento.getProntuario().getCpfPaciente();
        boolean deOutraInstituicao = !atendimento.getInstituicao().getCnpj().equals(cnpj);
        AuditoriaContexto.adicionar("cpf", cpf);
        AuditoriaContexto.adicionar("entreInstituicoes", deOutraInstituicao);
        AuditoriaContexto.adicionar("cnpjDono", atendimento.getInstituicao().getCnpj());
        if (!podeVer(cnpj, cpf).test(atendimento)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Sem acesso: este atendimento é de outra instituição e não há solicitação de acesso aprovada.");
        }
        return detalhar(atendimento, false);
    }

    /** Caso de uso 12 (enfermeiro): prescricoes do paciente que esta instituicao pode ver. */
    @Transactional(readOnly = true)
    public List<RegistroClinicoDto> medicamentos(String cnpjParam, String cpfParam, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirPapel(solicitante, cnpj, Papel.ENFERMEIRO);
        String cpf = paciente(cpfParam).getCpf();
        Predicate<Atendimento> pode = podeVer(cnpj, cpf);
        List<PrescricaoMedicamento> visiveis = prescricoes.listarPorPaciente(cpf).stream()
                .filter(p -> pode.test(p.getAtendimento()))
                .toList();
        AuditoriaContexto.adicionar("entreInstituicoes",
                cruzaInstituicoes(visiveis.stream().map(RegistroClinico::getAtendimento).toList(), cnpj));
        return visiveis.stream().map(p -> RegistroMapper.paraDto(p, List.of())).toList();
    }

    /** Caso de uso 13 (medico ou enfermeiro): requisicoes de exame, com o status de cada uma. */
    @Transactional(readOnly = true)
    public List<RegistroClinicoDto> requisicoes(String cnpjParam, String cpfParam, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirPapel(solicitante, cnpj, Papel.MEDICO, Papel.ENFERMEIRO);
        String cpf = paciente(cpfParam).getCpf();
        Predicate<Atendimento> pode = podeVer(cnpj, cpf);
        List<RequisicaoExame> visiveis = requisicoes.listarPorPaciente(cpf).stream()
                .filter(e -> pode.test(e.getAtendimento()))
                .toList();
        AuditoriaContexto.adicionar("entreInstituicoes",
                cruzaInstituicoes(visiveis.stream().map(RegistroClinico::getAtendimento).toList(), cnpj));
        return requisicoesParaDto(visiveis, false);
    }

    /** Caso de uso 14 (medico): so as requisicoes que ja tem resultado lancado. */
    @Transactional(readOnly = true)
    public List<RegistroClinicoDto> exames(String cnpjParam, String cpfParam, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirPapel(solicitante, cnpj, Papel.MEDICO);
        String cpf = paciente(cpfParam).getCpf();
        Predicate<Atendimento> pode = podeVer(cnpj, cpf);
        List<RequisicaoExame> visiveis = requisicoes.listarPorPaciente(cpf).stream()
                .filter(e -> pode.test(e.getAtendimento()))
                .toList();
        AuditoriaContexto.adicionar("entreInstituicoes",
                cruzaInstituicoes(visiveis.stream().map(RegistroClinico::getAtendimento).toList(), cnpj));
        return requisicoesParaDto(visiveis, true);
    }

    // ================================================================ paciente

    @Transactional(readOnly = true)
    public List<AtendimentoHistoricoDto> historicoDoPaciente(Long idUsuario) {
        Paciente paciente = pacienteLogado(idUsuario);
        return atendimentos.listarPorPaciente(paciente.getCpf(), PageRequest.of(0, 100)).stream()
                .map(ProntuarioService::resumo)
                .toList();
    }

    /** Atendimento de outro paciente aparece como inexistente. */
    @Transactional(readOnly = true)
    public ProntuarioDetalhadoDto prontuarioDoPaciente(Long idUsuario, Long idAtendimento) {
        Paciente paciente = pacienteLogado(idUsuario);
        Atendimento atendimento = atendimentos.findById(idAtendimento)
                .filter(a -> a.getProntuario().getCpfPaciente().equals(paciente.getCpf()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atendimento não encontrado."));
        return detalhar(atendimento, true);
    }

    @Transactional(readOnly = true)
    public List<RegistroClinicoDto> medicamentosDoPaciente(Long idUsuario) {
        Paciente paciente = pacienteLogado(idUsuario);
        return prescricoes.listarPorPaciente(paciente.getCpf()).stream()
                .filter(PrescricaoMedicamento::isVisivelAoPaciente)
                .map(p -> RegistroMapper.paraDto(p, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistroClinicoDto> requisicoesDoPaciente(Long idUsuario) {
        Paciente paciente = pacienteLogado(idUsuario);
        return requisicoesParaDto(requisicoes.listarPorPaciente(paciente.getCpf()).stream()
                .filter(RequisicaoExame::isVisivelAoPaciente)
                .toList(), false);
    }

    @Transactional(readOnly = true)
    public List<RegistroClinicoDto> examesDoPaciente(Long idUsuario) {
        Paciente paciente = pacienteLogado(idUsuario);
        return requisicoesParaDto(requisicoes.listarPorPaciente(paciente.getCpf()).stream()
                .filter(RequisicaoExame::isVisivelAoPaciente)
                .toList(), true);
    }

    // ================================================================ ajudantes

    /** Atendimento visivel para a instituicao: e dela, ou ela tem acesso aprovado ao paciente. */
    private Predicate<Atendimento> podeVer(String cnpj, String cpf) {
        boolean acessoAprovado = solicitacoes.buscar(cpf, cnpj, StatusSolicitacao.APROVADA).isPresent();
        return a -> acessoAprovado || a.getInstituicao().getCnpj().equals(cnpj);
    }

    /** Verdadeiro se algum dos atendimentos e de outra instituicao (acesso garantido por solicitacao aprovada). */
    private static boolean cruzaInstituicoes(Collection<Atendimento> lista, String cnpj) {
        return lista.stream().anyMatch(a -> !a.getInstituicao().getCnpj().equals(cnpj));
    }

    private Paciente paciente(String cpfParam) {
        return pacientes.findById(Cpf.normalizar(cpfParam))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente não encontrado."));
    }

    private Paciente pacienteLogado(Long idUsuario) {
        permissao.exigirUsuarioApto(idUsuario);
        return pacientes.buscarPorUsuario(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito a pacientes."));
    }

    private ProntuarioDetalhadoDto detalhar(Atendimento atendimento, boolean somenteVisiveisAoPaciente) {
        List<RegistroClinico> lista = registros.listarPorAtendimento(atendimento.getId()).stream()
                .filter(r -> !somenteVisiveisAoPaciente || r.isVisivelAoPaciente())
                .toList();

        List<Long> idsRequisicoes = new ArrayList<>();
        for (RegistroClinico r : lista) {
            if (r instanceof RequisicaoExame) {
                idsRequisicoes.add(r.getId());
            }
        }
        Map<Long, List<ResultadoExame>> resultadosPorRequisicao = resultadosPorRequisicao(idsRequisicoes);

        List<RegistroClinicoDto> dtos = lista.stream()
                .map(r -> RegistroMapper.paraDto(r, resultadosPorRequisicao.getOrDefault(r.getId(), List.of())))
                .toList();

        Paciente paciente = atendimento.getProntuario().getPaciente();
        return new ProntuarioDetalhadoDto(resumo(atendimento), paciente.getCpf(),
                paciente.getUsuario().getNome(), dtos);
    }

    /** "somenteComResultado": para o caso de uso 14, descarta as requisicoes ainda sem resultado. */
    private List<RegistroClinicoDto> requisicoesParaDto(List<RequisicaoExame> lista, boolean somenteComResultado) {
        Map<Long, List<ResultadoExame>> porRequisicao = resultadosPorRequisicao(
                lista.stream().map(RequisicaoExame::getId).toList());
        return lista.stream()
                .filter(e -> !somenteComResultado || !porRequisicao.getOrDefault(e.getId(), List.of()).isEmpty())
                .map(e -> RegistroMapper.paraDto(e, porRequisicao.getOrDefault(e.getId(), List.of())))
                .toList();
    }

    private Map<Long, List<ResultadoExame>> resultadosPorRequisicao(Collection<Long> idsRequisicoes) {
        Map<Long, List<ResultadoExame>> mapa = new HashMap<>();
        if (idsRequisicoes.isEmpty()) {
            return mapa;
        }
        for (ResultadoExame resultado : resultados.listarPorRequisicoes(idsRequisicoes)) {
            mapa.computeIfAbsent(resultado.getId().getIdRequisicao(), k -> new ArrayList<>()).add(resultado);
        }
        return mapa;
    }

    private static AtendimentoHistoricoDto resumo(Atendimento a) {
        return new AtendimentoHistoricoDto(
                a.getId(),
                a.getInstituicao().getCnpj(),
                a.getInstituicao().getNome(),
                a.getMotivo(),
                a.getDataHora(),
                a.getDataFinalizacao(),
                a.getStatus(),
                a.getMedico() != null ? a.getMedico().getUsuario().getNome() : null);
    }
}
