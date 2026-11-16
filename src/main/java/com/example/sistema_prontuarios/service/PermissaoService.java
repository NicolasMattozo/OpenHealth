package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Instituicao;
import com.example.sistema_prontuarios.domain.Usuario;
import com.example.sistema_prontuarios.domain.UsuarioInstituicao;
import com.example.sistema_prontuarios.domain.UsuarioInstituicaoId;
import com.example.sistema_prontuarios.domain.UsuarioPapelId;
import com.example.sistema_prontuarios.domain.enums.Papel;
import com.example.sistema_prontuarios.domain.enums.TipoProfissional;
import com.example.sistema_prontuarios.repository.InstituicaoRepository;
import com.example.sistema_prontuarios.repository.UsuarioInstituicaoRepository;
import com.example.sistema_prontuarios.repository.UsuarioPapelRepository;
import com.example.sistema_prontuarios.repository.UsuarioRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Confere as permissoes no banco a cada operacao administrativa (nao so no token),
 * para que quem perdeu o acesso nao continue operando ate o token expirar.
 */
@Service
public class PermissaoService {

    private final UsuarioRepository usuarios;
    private final UsuarioInstituicaoRepository vinculos;
    private final UsuarioPapelRepository papeis;
    private final InstituicaoRepository instituicoes;

    public PermissaoService(UsuarioRepository usuarios,
                            UsuarioInstituicaoRepository vinculos,
                            UsuarioPapelRepository papeis,
                            InstituicaoRepository instituicoes) {
        this.usuarios = usuarios;
        this.vinculos = vinculos;
        this.papeis = papeis;
        this.instituicoes = instituicoes;
    }

    @Transactional(readOnly = true)
    public boolean isAdminSistema(Long idUsuario) {
        return usuarioApto(idUsuario).map(Usuario::isAdministradorSistema).orElse(false);
    }

    @Transactional(readOnly = true)
    public void exigirAdminSistema(Long idUsuario) {
        if (!isAdminSistema(idUsuario)) {
            throw negado();
        }
    }

    /** Admin do sistema, ou admin da instituicao com vinculo e instituicao ativos. */
    @Transactional(readOnly = true)
    public void exigirAdminDaInstituicao(Long idUsuario, String cnpj) {
        Optional<Usuario> usuario = usuarioApto(idUsuario);
        if (usuario.isEmpty()) {
            throw negado();
        }
        if (usuario.get().isAdministradorSistema()) {
            return;
        }
        boolean vinculoAtivo = vinculos.findById(new UsuarioInstituicaoId(idUsuario, cnpj))
                .map(UsuarioInstituicao::isAtivo).orElse(false);
        boolean admin = papeis.existsById(new UsuarioPapelId(idUsuario, cnpj, Papel.ADMIN_INSTITUICAO));
        boolean instituicaoAtiva = instituicoes.findById(cnpj).map(Instituicao::isAtivo).orElse(false);
        if (!(vinculoAtivo && admin && instituicaoAtiva)) {
            throw negado();
        }
    }

    /**
     * Exige que o usuario exerca pelo menos um dos papeis informados na instituicao, com vinculo e
     * instituicao ativos. Nao ha excecao para administradores: quem administra nao opera a recepcao.
     */
    @Transactional(readOnly = true)
    public void exigirPapel(Long idUsuario, String cnpj, Papel... aceitos) {
        if (usuarioApto(idUsuario).isEmpty()) {
            throw negado();
        }
        boolean vinculoAtivo = vinculos.findById(new UsuarioInstituicaoId(idUsuario, cnpj))
                .map(UsuarioInstituicao::isAtivo).orElse(false);
        boolean instituicaoAtiva = instituicoes.findById(cnpj).map(Instituicao::isAtivo).orElse(false);
        boolean temPapel = false;
        for (Papel papel : aceitos) {
            if (papeis.existsById(new UsuarioPapelId(idUsuario, cnpj, papel))) {
                temPapel = true;
            }
        }
        if (!(vinculoAtivo && instituicaoAtiva && temPapel)) {
            throw negado();
        }
    }

    /**
     * Exige medico ou enfermeiro da instituicao (vinculo e instituicao ativos) e diz qual dos dois e.
     * Um usuario nao acumula os dois perfis na mesma instituicao.
     */
    @Transactional(readOnly = true)
    public TipoProfissional exigirProfissional(Long idUsuario, String cnpj) {
        exigirPapel(idUsuario, cnpj, Papel.MEDICO, Papel.ENFERMEIRO);
        return papeis.existsById(new UsuarioPapelId(idUsuario, cnpj, Papel.MEDICO))
                ? TipoProfissional.MEDICO
                : TipoProfissional.ENFERMEIRO;
    }

    /** Qualquer usuario logado, mas ativo e que ja trocou a senha temporaria. */
    @Transactional(readOnly = true)
    public void exigirUsuarioApto(Long idUsuario) {
        if (usuarioApto(idUsuario).isEmpty()) {
            throw negado();
        }
    }

    /** Usuario ativo e que ja trocou a senha temporaria. */
    private Optional<Usuario> usuarioApto(Long idUsuario) {
        return usuarios.findById(idUsuario).filter(u -> u.isAtivo() && !u.isDeveTrocarSenha());
    }

    private ResponseStatusException negado() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para esta operação.");
    }
}
