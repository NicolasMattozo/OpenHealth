package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Paciente;
import com.example.sistema_prontuarios.domain.SolicitacaoAcesso;
import com.example.sistema_prontuarios.domain.enums.StatusSolicitacao;
import com.example.sistema_prontuarios.repository.PacienteRepository;
import com.example.sistema_prontuarios.repository.SolicitacaoAcessoRepository;
import com.example.sistema_prontuarios.web.dto.SolicitacaoParaPacienteDto;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.sistema_prontuarios.audit.AuditoriaContexto;

/**
 * Casos de uso 15 a 17: o paciente decide quem acessa os seus dados.
 * Cada paciente so enxerga e decide as proprias solicitacoes.
 */
@Service
public class AcessoPacienteService {

    private final SolicitacaoAcessoRepository solicitacoes;
    private final PacienteRepository pacientes;
    private final PermissaoService permissao;

    public AcessoPacienteService(SolicitacaoAcessoRepository solicitacoes,
                                 PacienteRepository pacientes,
                                 PermissaoService permissao) {
        this.solicitacoes = solicitacoes;
        this.pacientes = pacientes;
        this.permissao = permissao;
    }

    /** As 100 solicitacoes mais recentes: pendentes, aprovadas (acessos concedidos), negadas e revogadas. */
    @Transactional(readOnly = true)
    public List<SolicitacaoParaPacienteDto> listar(Long idUsuario) {
        Paciente paciente = pacienteLogado(idUsuario);
        return solicitacoes.listarPorPaciente(paciente.getCpf(), PageRequest.of(0, 100)).stream()
                .map(AcessoPacienteService::paraDto)
                .toList();
    }

    /** Caso de uso 16: PENDENTE -> APROVADA; a instituicao passa a ter acesso aos dados. */
    @Transactional
    public SolicitacaoParaPacienteDto aprovar(Long idUsuario, Long idSolicitacao) {
        SolicitacaoAcesso solicitacao = carregar(pacienteLogado(idUsuario), idSolicitacao);
        exigirStatus(solicitacao, StatusSolicitacao.PENDENTE, "Só é possível aprovar uma solicitação pendente.");
        if (!solicitacao.getInstituicao().isAtivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A instituição solicitante está inativa.");
        }
        solicitacao.setStatus(StatusSolicitacao.APROVADA);
        solicitacao.setDataDecisao(Instant.now());
        return paraDto(solicitacao);
    }

    /** Caso de uso 17: PENDENTE -> NEGADA; nenhum acesso e concedido. */
    @Transactional
    public SolicitacaoParaPacienteDto negar(Long idUsuario, Long idSolicitacao) {
        SolicitacaoAcesso solicitacao = carregar(pacienteLogado(idUsuario), idSolicitacao);
        exigirStatus(solicitacao, StatusSolicitacao.PENDENTE, "Só é possível negar uma solicitação pendente.");
        solicitacao.setStatus(StatusSolicitacao.NEGADA);
        solicitacao.setDataDecisao(Instant.now());
        return paraDto(solicitacao);
    }

    /** O paciente retira um acesso ja concedido: APROVADA -> REVOGADA. A instituicao pode pedir de novo. */
    @Transactional
    public SolicitacaoParaPacienteDto revogar(Long idUsuario, Long idSolicitacao) {
        SolicitacaoAcesso solicitacao = carregar(pacienteLogado(idUsuario), idSolicitacao);
        exigirStatus(solicitacao, StatusSolicitacao.APROVADA, "Só é possível revogar um acesso aprovado.");
        solicitacao.setStatus(StatusSolicitacao.REVOGADA);
        solicitacao.setDataRevogacao(Instant.now());
        return paraDto(solicitacao);
    }

    private Paciente pacienteLogado(Long idUsuario) {
        permissao.exigirUsuarioApto(idUsuario);
        Paciente paciente = pacientes.buscarPorUsuario(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito a pacientes."));
        AuditoriaContexto.adicionar("cpf", paciente.getCpf());
        return paciente;
    }

    /** Solicitacao de outro paciente aparece como inexistente, sem revelar que existe. */
    private SolicitacaoAcesso carregar(Paciente paciente, Long idSolicitacao) {
        return solicitacoes.findById(idSolicitacao)
                .filter(s -> s.getPaciente().getCpf().equals(paciente.getCpf()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));
    }

    private static void exigirStatus(SolicitacaoAcesso solicitacao, StatusSolicitacao esperado, String mensagem) {
        if (solicitacao.getStatus() != esperado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
        }
    }

    private static SolicitacaoParaPacienteDto paraDto(SolicitacaoAcesso s) {
        return new SolicitacaoParaPacienteDto(
                s.getId(),
                s.getInstituicao().getCnpj(),
                s.getInstituicao().getNome(),
                s.getInstituicao().getTipo(),
                s.getFinalidade(),
                s.getRecepcionista().getNome(),
                s.getStatus(),
                s.getDataSolicitacao(),
                s.getDataDecisao(),
                s.getDataRevogacao());
    }
}
