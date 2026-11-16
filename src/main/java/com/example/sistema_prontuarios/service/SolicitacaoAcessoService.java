package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Instituicao;
import com.example.sistema_prontuarios.domain.Paciente;
import com.example.sistema_prontuarios.domain.SolicitacaoAcesso;
import com.example.sistema_prontuarios.domain.Usuario;
import com.example.sistema_prontuarios.domain.enums.Papel;
import com.example.sistema_prontuarios.domain.enums.StatusSolicitacao;
import com.example.sistema_prontuarios.repository.InstituicaoRepository;
import com.example.sistema_prontuarios.repository.PacienteRepository;
import com.example.sistema_prontuarios.repository.SolicitacaoAcessoRepository;
import com.example.sistema_prontuarios.repository.UsuarioRepository;
import com.example.sistema_prontuarios.util.Cnpj;
import com.example.sistema_prontuarios.util.Cpf;
import com.example.sistema_prontuarios.web.dto.SolicitacaoAcessoDto;
import com.example.sistema_prontuarios.web.dto.SolicitacaoAcessoRequest;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.sistema_prontuarios.audit.AuditoriaContexto;

/** Caso de uso 18: a recepcao solicita, em nome da instituicao, acesso aos dados do paciente. */
@Service
public class SolicitacaoAcessoService {

    private final SolicitacaoAcessoRepository solicitacoes;
    private final InstituicaoRepository instituicoes;
    private final PacienteRepository pacientes;
    private final UsuarioRepository usuarios;
    private final PermissaoService permissao;

    public SolicitacaoAcessoService(SolicitacaoAcessoRepository solicitacoes,
                                    InstituicaoRepository instituicoes,
                                    PacienteRepository pacientes,
                                    UsuarioRepository usuarios,
                                    PermissaoService permissao) {
        this.solicitacoes = solicitacoes;
        this.instituicoes = instituicoes;
        this.pacientes = pacientes;
        this.usuarios = usuarios;
        this.permissao = permissao;
    }

    /** Cria a solicitacao com status PENDENTE; a aprovacao e do paciente (casos de uso 15 a 17). */
    @Transactional
    public SolicitacaoAcessoDto solicitar(String cnpjParam, SolicitacaoAcessoRequest requisicao, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirPapel(solicitante, cnpj, Papel.RECEPCIONISTA);

        String cpf = Cpf.normalizar(requisicao.cpfPaciente());
        AuditoriaContexto.adicionar("cpf", cpf);
        Paciente paciente = pacientes.findById(cpf)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente não encontrado."));

        if (solicitacoes.buscar(cpf, cnpj, StatusSolicitacao.APROVADA).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A instituição já possui acesso aos dados deste paciente.");
        }
        if (solicitacoes.buscar(cpf, cnpj, StatusSolicitacao.PENDENTE).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma solicitação pendente deste paciente para a instituição.");
        }

        Instituicao instituicao = instituicoes.findById(cnpj)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instituição não encontrada."));
        Usuario recepcionista = usuarios.findById(solicitante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inexistente."));

        SolicitacaoAcesso solicitacao = new SolicitacaoAcesso();
        solicitacao.setPaciente(paciente);
        solicitacao.setInstituicao(instituicao);
        solicitacao.setRecepcionista(recepcionista);
        solicitacao.setFinalidade(requisicao.finalidade().trim());
        solicitacoes.save(solicitacao);
        AuditoriaContexto.adicionar("idSolicitacao", solicitacao.getId());

        return paraDto(solicitacao);
    }

    /** As 50 solicitacoes mais recentes da instituicao. */
    @Transactional(readOnly = true)
    public List<SolicitacaoAcessoDto> listar(String cnpjParam, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirPapel(solicitante, cnpj, Papel.RECEPCIONISTA);
        return solicitacoes.listarPorInstituicao(cnpj, PageRequest.of(0, 50)).stream()
                .map(SolicitacaoAcessoService::paraDto)
                .toList();
    }

    private static SolicitacaoAcessoDto paraDto(SolicitacaoAcesso s) {
        return new SolicitacaoAcessoDto(
                s.getId(),
                s.getPaciente().getCpf(),
                s.getPaciente().getUsuario().getNome(),
                s.getInstituicao().getCnpj(),
                s.getFinalidade(),
                s.getStatus(),
                s.getDataSolicitacao(),
                s.getDataDecisao(),
                s.getDataRevogacao(),
                s.getRecepcionista().getNome());
    }
}
