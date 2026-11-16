package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Instituicao;
import com.example.sistema_prontuarios.repository.InstituicaoRepository;
import com.example.sistema_prontuarios.util.Cnpj;
import com.example.sistema_prontuarios.web.dto.InstituicaoDto;
import com.example.sistema_prontuarios.web.dto.InstituicaoRequest;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.sistema_prontuarios.audit.AuditoriaContexto;

/** Casos de uso 23 a 25: gerenciar instituicoes (somente o administrador do sistema). */
@Service
public class InstituicaoService {

    private final InstituicaoRepository instituicoes;
    private final PermissaoService permissao;

    public InstituicaoService(InstituicaoRepository instituicoes, PermissaoService permissao) {
        this.instituicoes = instituicoes;
        this.permissao = permissao;
    }

    @Transactional(readOnly = true)
    public List<InstituicaoDto> listar(Long solicitante) {
        permissao.exigirAdminSistema(solicitante);
        return instituicoes.findAll(Sort.by("nome")).stream().map(InstituicaoService::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public InstituicaoDto buscar(String cnpj, Long solicitante) {
        permissao.exigirAdminSistema(solicitante);
        return paraDto(carregar(cnpj));
    }

    @Transactional
    public InstituicaoDto cadastrar(InstituicaoRequest requisicao, Long solicitante) {
        permissao.exigirAdminSistema(solicitante);

        String cnpj = Cnpj.normalizar(requisicao.cnpj());
        AuditoriaContexto.adicionar("cnpj", cnpj);
        if (!Cnpj.valido(cnpj)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CNPJ inválido.");
        }
        if (instituicoes.existsById(cnpj)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma instituição com este CNPJ.");
        }

        Instituicao instituicao = new Instituicao();
        instituicao.setCnpj(cnpj);
        instituicao.setNome(requisicao.nome().trim());
        instituicao.setTipo(vazioParaNulo(requisicao.tipo()));
        instituicao.setEndereco(vazioParaNulo(requisicao.endereco()));
        return paraDto(instituicoes.save(instituicao));
    }

    /** "Remover" instituicao e inativar: prontuarios e auditoria continuam referenciando-a. */
    @Transactional
    public InstituicaoDto alterarAtivo(String cnpj, boolean ativo, Long solicitante) {
        permissao.exigirAdminSistema(solicitante);
        Instituicao instituicao = carregar(cnpj);
        instituicao.setAtivo(ativo);
        return paraDto(instituicao);
    }

    private Instituicao carregar(String cnpj) {
        return instituicoes.findById(Cnpj.normalizar(cnpj))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instituição não encontrada."));
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static InstituicaoDto paraDto(Instituicao i) {
        return new InstituicaoDto(i.getCnpj(), i.getNome(), i.getTipo(), i.getEndereco(), i.isAtivo());
    }
}
