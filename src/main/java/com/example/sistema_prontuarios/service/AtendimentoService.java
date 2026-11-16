package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Atendimento;
import com.example.sistema_prontuarios.domain.Instituicao;
import com.example.sistema_prontuarios.domain.Paciente;
import com.example.sistema_prontuarios.domain.Prontuario;
import com.example.sistema_prontuarios.domain.SolicitacaoAcesso;
import com.example.sistema_prontuarios.domain.Usuario;
import com.example.sistema_prontuarios.domain.enums.Papel;
import com.example.sistema_prontuarios.domain.enums.StatusAtendimento;
import com.example.sistema_prontuarios.domain.enums.StatusSolicitacao;
import com.example.sistema_prontuarios.repository.AtendimentoRepository;
import com.example.sistema_prontuarios.repository.InstituicaoRepository;
import com.example.sistema_prontuarios.repository.PacienteRepository;
import com.example.sistema_prontuarios.repository.ProntuarioRepository;
import com.example.sistema_prontuarios.repository.SolicitacaoAcessoRepository;
import com.example.sistema_prontuarios.repository.UsuarioRepository;
import com.example.sistema_prontuarios.util.Cnpj;
import com.example.sistema_prontuarios.util.Cpf;
import com.example.sistema_prontuarios.web.dto.AtendimentoDto;
import com.example.sistema_prontuarios.web.dto.AtendimentoRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.sistema_prontuarios.audit.AuditoriaContexto;

/** Caso de uso 4: registrar a abertura de prontuario/atendimento. */
@Service
public class AtendimentoService {

    private final AtendimentoRepository atendimentos;
    private final InstituicaoRepository instituicoes;
    private final PacienteRepository pacientes;
    private final ProntuarioRepository prontuarios;
    private final SolicitacaoAcessoRepository solicitacoes;
    private final UsuarioRepository usuarios;
    private final PermissaoService permissao;

    @PersistenceContext
    private EntityManager em;

    public AtendimentoService(AtendimentoRepository atendimentos,
                              InstituicaoRepository instituicoes,
                              PacienteRepository pacientes,
                              ProntuarioRepository prontuarios,
                              SolicitacaoAcessoRepository solicitacoes,
                              UsuarioRepository usuarios,
                              PermissaoService permissao) {
        this.atendimentos = atendimentos;
        this.instituicoes = instituicoes;
        this.pacientes = pacientes;
        this.prontuarios = prontuarios;
        this.solicitacoes = solicitacoes;
        this.usuarios = usuarios;
        this.permissao = permissao;
    }

    /**
     * Abre o atendimento do paciente na instituicao. Se o paciente ainda nao tem prontuario, ele e criado agora.
     * Se a instituicao ja tem uma solicitacao de acesso aprovada para o paciente, o atendimento fica ligado a ela.
     */
    @Transactional
    public AtendimentoDto abrir(String cnpjParam, AtendimentoRequest requisicao, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirPapel(solicitante, cnpj, Papel.RECEPCIONISTA);

        String cpf = Cpf.normalizar(requisicao.cpfPaciente());
        AuditoriaContexto.adicionar("cpf", cpf);
        Paciente paciente = pacientes.findById(cpf)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paciente não encontrado. Cadastre o paciente antes de abrir o atendimento."));

        if (atendimentos.contarPorStatus(cpf, cnpj, StatusAtendimento.ABERTO, StatusAtendimento.EM_ANDAMENTO) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este paciente já tem um atendimento em aberto nesta instituição.");
        }

        Instituicao instituicao = instituicoes.findById(cnpj)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instituição não encontrada."));
        Usuario recepcionista = usuarios.findById(solicitante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inexistente."));

        Prontuario prontuario = prontuarios.findById(cpf).orElse(null);
        if (prontuario == null) {
            prontuario = new Prontuario();
            prontuario.setPaciente(paciente);
            em.persist(prontuario);
        }

        SolicitacaoAcesso aprovada = solicitacoes.buscar(cpf, cnpj, StatusSolicitacao.APROVADA).orElse(null);

        Atendimento atendimento = new Atendimento();
        atendimento.setProntuario(prontuario);
        atendimento.setInstituicao(instituicao);
        atendimento.setSolicitacao(aprovada);
        atendimento.setRecepcionista(recepcionista);
        atendimento.setMotivo(requisicao.motivo().trim());
        atendimentos.save(atendimento);
        AuditoriaContexto.adicionar("idAtendimento", atendimento.getId());

        return paraDto(atendimento);
    }

    /** Os 50 atendimentos mais recentes da instituicao (opcionalmente so de um status). */
    @Transactional(readOnly = true)
    public List<AtendimentoDto> listar(String cnpjParam, StatusAtendimento status, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirPapel(solicitante, cnpj, Papel.RECEPCIONISTA, Papel.MEDICO, Papel.ENFERMEIRO);

        return atendimentos.listarRecentes(cnpj, PageRequest.of(0, 50)).stream()
                .filter(a -> status == null || a.getStatus() == status)
                .map(AtendimentoService::paraDto)
                .toList();
    }

    static AtendimentoDto paraDto(Atendimento a) {
        Paciente paciente = a.getProntuario().getPaciente();
        return new AtendimentoDto(
                a.getId(),
                paciente.getCpf(),
                paciente.getUsuario().getNome(),
                a.getMotivo(),
                a.getDataHora(),
                a.getDataFinalizacao(),
                a.getStatus(),
                a.getSolicitacao() != null ? a.getSolicitacao().getId() : null,
                a.getRecepcionista().getNome(),
                a.getMedico() != null ? a.getMedico().getUsuario().getNome() : null);
    }
}
