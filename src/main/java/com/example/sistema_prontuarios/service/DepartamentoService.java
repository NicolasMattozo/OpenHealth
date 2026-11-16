package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Departamento;
import com.example.sistema_prontuarios.domain.Instituicao;
import com.example.sistema_prontuarios.repository.DepartamentoRepository;
import com.example.sistema_prontuarios.repository.InstituicaoRepository;
import com.example.sistema_prontuarios.util.Cnpj;
import com.example.sistema_prontuarios.web.dto.DepartamentoDto;
import com.example.sistema_prontuarios.web.dto.DepartamentoRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.sistema_prontuarios.audit.AuditoriaContexto;

/** Casos de uso 26 e 27. Listar: admin do sistema ou da instituicao; criar/inativar: admin do sistema. */
@Service
public class DepartamentoService {

    private final DepartamentoRepository departamentos;
    private final InstituicaoRepository instituicoes;
    private final PermissaoService permissao;

    public DepartamentoService(DepartamentoRepository departamentos,
                               InstituicaoRepository instituicoes,
                               PermissaoService permissao) {
        this.departamentos = departamentos;
        this.instituicoes = instituicoes;
        this.permissao = permissao;
    }

    @Transactional(readOnly = true)
    public List<DepartamentoDto> listar(String cnpjParam, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirAdminDaInstituicao(solicitante, cnpj);
        carregarInstituicao(cnpj);
        return departamentos.listarPorInstituicao(cnpj).stream().map(DepartamentoService::paraDto).toList();
    }

    @Transactional
    public DepartamentoDto criar(String cnpjParam, DepartamentoRequest requisicao, Long solicitante) {
        permissao.exigirAdminSistema(solicitante);
        String cnpj = Cnpj.normalizar(cnpjParam);
        Instituicao instituicao = carregarInstituicao(cnpj);
        if (!instituicao.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A instituição está inativa.");
        }

        String nome = requisicao.nome().trim();
        if (departamentos.contarPorNome(cnpj, nome) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um departamento com este nome na instituição.");
        }

        Departamento departamento = new Departamento();
        departamento.setInstituicao(instituicao);
        departamento.setNome(nome);
        Departamento salvo = departamentos.save(departamento);
        AuditoriaContexto.adicionar("idDepartamento", salvo.getId());
        return paraDto(salvo);
    }

    /** "Remover" departamento e inativar. Os usuarios vinculados continuam apontando para ele. */
    @Transactional
    public DepartamentoDto alterarAtivo(String cnpjParam, Long id, boolean ativo, Long solicitante) {
        permissao.exigirAdminSistema(solicitante);
        String cnpj = Cnpj.normalizar(cnpjParam);
        Departamento departamento = departamentos.findById(id)
                .filter(d -> d.getInstituicao().getCnpj().equals(cnpj))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento não encontrado."));
        departamento.setAtivo(ativo);
        return paraDto(departamento);
    }

    private Instituicao carregarInstituicao(String cnpj) {
        return instituicoes.findById(cnpj)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instituição não encontrada."));
    }

    private static DepartamentoDto paraDto(Departamento d) {
        return new DepartamentoDto(d.getId(), d.getInstituicao().getCnpj(), d.getNome(), d.isAtivo());
    }
}
