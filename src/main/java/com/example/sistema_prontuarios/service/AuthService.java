package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Paciente;
import com.example.sistema_prontuarios.domain.Usuario;
import com.example.sistema_prontuarios.domain.UsuarioInstituicao;
import com.example.sistema_prontuarios.domain.UsuarioPapel;
import com.example.sistema_prontuarios.repository.PacienteRepository;
import com.example.sistema_prontuarios.repository.UsuarioInstituicaoRepository;
import com.example.sistema_prontuarios.repository.UsuarioPapelRepository;
import com.example.sistema_prontuarios.repository.UsuarioRepository;
import com.example.sistema_prontuarios.security.JwtService;
import com.example.sistema_prontuarios.security.TentativasLoginService;
import com.example.sistema_prontuarios.web.dto.LoginResponse;
import com.example.sistema_prontuarios.web.dto.UsuarioLogadoDto;
import com.example.sistema_prontuarios.web.dto.VinculoDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.sistema_prontuarios.audit.AuditoriaContexto;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PacienteRepository pacientes;
    private final UsuarioInstituicaoRepository vinculos;
    private final UsuarioPapelRepository papeis;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final TentativasLoginService tentativasService;

    /** Hash usado quando o usuario nao existe, para o login levar o mesmo tempo nos dois casos. */
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios,
                       PacienteRepository pacientes,
                       UsuarioInstituicaoRepository vinculos,
                       UsuarioPapelRepository papeis,
                       PasswordEncoder encoder,
                       JwtService jwtService,
                       TentativasLoginService tentativasService) {
        this.usuarios = usuarios;
        this.pacientes = pacientes;
        this.vinculos = vinculos;
        this.papeis = papeis;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.tentativasService = tentativasService;
        this.hashFalso = encoder.encode("senha-falsa-para-igualar-o-tempo");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String identificador, String senha) {
        if (tentativasService.estaBloqueado(identificador)) {
            long minutos = tentativasService.minutosRestantes(identificador);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas incorretas. Tente novamente em " + minutos + " minuto(s).");
        }

        Usuario usuario = localizar(identificador.trim());

        boolean temSenha = usuario != null && usuario.getSenhaHash() != null;
        boolean senhaConfere = encoder.matches(senha, temSenha ? usuario.getSenhaHash() : hashFalso);

        if (usuario == null || !usuario.isAtivo() || !temSenha || !senhaConfere) {
            tentativasService.registrarFalha(identificador);
            if (usuario != null) {
                AuditoriaContexto.adicionar("idUsuarioAlvo", usuario.getId());
            }
            if (tentativasService.estaBloqueado(identificador)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Número máximo de 5 tentativas excedido. Bloqueado por 15 minutos.");
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }

        tentativasService.limpar(identificador);
        AuditoriaContexto.definirUsuario(usuario.getId());
        return respostaDeLogin(montar(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioLogadoDto usuarioLogado(Long idUsuario) {
        return montar(usuarioAtivo(idUsuario));
    }

    /**
     * Troca a senha e devolve um token novo. Enquanto o usuario estiver com a senha temporaria,
     * o token antigo nao carrega papeis; este novo ja carrega.
     */
    @Transactional
    public LoginResponse trocarSenha(Long idUsuario, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioAtivo(idUsuario);

        if (usuario.getSenhaHash() == null || !encoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta.");
        }
        if (senhaAtual.equals(novaSenha)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nova senha deve ser diferente da atual.");
        }

        usuario.setSenhaHash(encoder.encode(novaSenha));
        usuario.setDeveTrocarSenha(false);
        return respostaDeLogin(montar(usuario));
    }

    private LoginResponse respostaDeLogin(UsuarioLogadoDto dados) {
        return new LoginResponse(jwtService.gerar(dados), "Bearer", jwtService.getExpiracaoSegundos(), dados);
    }

    private Usuario usuarioAtivo(Long idUsuario) {
        return usuarios.findById(idUsuario)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inativo ou inexistente."));
    }

    /** E-mail para qualquer usuario; CPF (11 digitos) para paciente. */
    private Usuario localizar(String identificador) {
        if (identificador.contains("@")) {
            return usuarios.buscarPorEmail(identificador).orElse(null);
        }
        String cpf = identificador.replaceAll("\\D", "");
        if (cpf.length() == 11) {
            return pacientes.findById(cpf).map(Paciente::getUsuario).orElse(null);
        }
        return null;
    }

    private UsuarioLogadoDto montar(Usuario usuario) {
        if (usuario.isDeveTrocarSenha()) {
            return new UsuarioLogadoDto(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                    null, List.of(), List.of(), true);
        }

        List<String> roles = new ArrayList<>();
        if (usuario.isAdministradorSistema()) {
            roles.add("ROLE_ADMIN_SISTEMA");
        }

        String cpf = pacientes.buscarPorUsuario(usuario.getId()).map(Paciente::getCpf).orElse(null);
        if (cpf != null) {
            roles.add("ROLE_PACIENTE");
        }

        List<UsuarioPapel> todosPapeis = papeis.listarPorUsuario(usuario.getId());
        List<VinculoDto> instituicoes = new ArrayList<>();
        for (UsuarioInstituicao vinculo : vinculos.listarAtivosPorUsuario(usuario.getId())) {
            String cnpj = vinculo.getId().getCnpjInstituicao();
            List<String> papeisNaInstituicao = todosPapeis.stream()
                    .filter(p -> p.getId().getCnpjInstituicao().equals(cnpj))
                    .map(p -> p.getId().getPapel().name())
                    .sorted()
                    .toList();
            instituicoes.add(new VinculoDto(cnpj, vinculo.getInstituicao().getNome(), papeisNaInstituicao));
            for (String papel : papeisNaInstituicao) {
                String role = "ROLE_" + papel;
                if (!roles.contains(role)) {
                    roles.add(role);
                }
            }
        }

        return new UsuarioLogadoDto(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                cpf, roles, instituicoes, false);
    }
}