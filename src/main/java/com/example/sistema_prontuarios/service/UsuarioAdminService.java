package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Departamento;
import com.example.sistema_prontuarios.domain.Instituicao;
import com.example.sistema_prontuarios.domain.ProfissionalSaude;
import com.example.sistema_prontuarios.domain.ProfissionalSaudeId;
import com.example.sistema_prontuarios.domain.Usuario;
import com.example.sistema_prontuarios.domain.UsuarioInstituicao;
import com.example.sistema_prontuarios.domain.UsuarioInstituicaoId;
import com.example.sistema_prontuarios.domain.UsuarioPapel;
import com.example.sistema_prontuarios.domain.UsuarioPapelId;
import com.example.sistema_prontuarios.domain.enums.Papel;
import com.example.sistema_prontuarios.domain.enums.TipoProfissional;
import com.example.sistema_prontuarios.repository.DepartamentoRepository;
import com.example.sistema_prontuarios.repository.InstituicaoRepository;
import com.example.sistema_prontuarios.repository.ProfissionalSaudeRepository;
import com.example.sistema_prontuarios.repository.UsuarioInstituicaoRepository;
import com.example.sistema_prontuarios.repository.UsuarioPapelRepository;
import com.example.sistema_prontuarios.repository.UsuarioRepository;
import com.example.sistema_prontuarios.util.Cnpj;
import com.example.sistema_prontuarios.web.dto.UsuarioCadastroRequest;
import com.example.sistema_prontuarios.web.dto.UsuarioCriadoDto;
import com.example.sistema_prontuarios.web.dto.UsuarioInstituicaoDto;
import com.example.sistema_prontuarios.web.dto.UsuarioPapeisRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.sistema_prontuarios.audit.AuditoriaContexto;

/**
 * Casos de uso 19 a 22: gerenciar usuarios de uma instituicao.
 * Quem pode: administrador do sistema ou administrador da propria instituicao.
 */
@Service
public class UsuarioAdminService {

    /** Sem caracteres que se confundem (0/O, 1/l/I). */
    private static final String ALFABETO_SENHA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final InstituicaoRepository instituicoes;
    private final DepartamentoRepository departamentos;
    private final UsuarioRepository usuarios;
    private final UsuarioInstituicaoRepository vinculos;
    private final UsuarioPapelRepository papeis;
    private final ProfissionalSaudeRepository profissionais;
    private final PermissaoService permissao;
    private final PasswordEncoder encoder;

    @PersistenceContext
    private EntityManager em;

    public UsuarioAdminService(InstituicaoRepository instituicoes,
                               DepartamentoRepository departamentos,
                               UsuarioRepository usuarios,
                               UsuarioInstituicaoRepository vinculos,
                               UsuarioPapelRepository papeis,
                               ProfissionalSaudeRepository profissionais,
                               PermissaoService permissao,
                               PasswordEncoder encoder) {
        this.instituicoes = instituicoes;
        this.departamentos = departamentos;
        this.usuarios = usuarios;
        this.vinculos = vinculos;
        this.papeis = papeis;
        this.profissionais = profissionais;
        this.permissao = permissao;
        this.encoder = encoder;
    }

    // ------------------------------------------------------------------ listar

    @Transactional(readOnly = true)
    public List<UsuarioInstituicaoDto> listar(String cnpjParam, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirAdminDaInstituicao(solicitante, cnpj);
        carregarInstituicao(cnpj);

        List<UsuarioInstituicao> lista = vinculos.listarPorInstituicao(cnpj);

        Map<Long, List<String>> papeisPorUsuario = new HashMap<>();
        for (UsuarioPapel papel : papeis.listarPorInstituicao(cnpj)) {
            papeisPorUsuario
                    .computeIfAbsent(papel.getId().getIdUsuario(), k -> new ArrayList<>())
                    .add(papel.getId().getPapel().name());
        }

        List<Long> ids = lista.stream().map(v -> v.getId().getIdUsuario()).toList();
        Map<Long, List<ProfissionalSaude>> profissionaisPorUsuario = new HashMap<>();
        if (!ids.isEmpty()) {
            for (ProfissionalSaude p : profissionais.listarPorUsuarios(ids)) {
                profissionaisPorUsuario
                        .computeIfAbsent(p.getId().getIdUsuario(), k -> new ArrayList<>())
                        .add(p);
            }
        }

        return lista.stream()
                .map(v -> {
                    Long id = v.getId().getIdUsuario();
                    List<String> papeisDoUsuario = ordenar(papeisPorUsuario.get(id));
                    return paraDto(v, papeisDoUsuario, escolherProfissional(profissionaisPorUsuario.get(id), papeisDoUsuario));
                })
                .sorted(Comparator.comparing(UsuarioInstituicaoDto::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // ---------------------------------------------------------------- cadastrar

    /**
     * Se o e-mail ainda nao existe, cria o usuario com senha temporaria (devolvida uma unica vez).
     * Se ja existe (ex.: medico que atende em dois hospitais), apenas cria o vinculo com esta instituicao.
     */
    @Transactional
    public UsuarioCriadoDto cadastrar(String cnpjParam, UsuarioCadastroRequest requisicao, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirAdminDaInstituicao(solicitante, cnpj);
        Instituicao instituicao = carregarInstituicao(cnpj);
        if (!instituicao.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A instituição está inativa.");
        }

        Set<Papel> papeisDesejados = new HashSet<>(requisicao.papeis());
        TipoProfissional tipo = tipoProfissional(papeisDesejados);
        Departamento departamento = departamentoDaInstituicao(cnpj, requisicao.departamentoId());

        Usuario usuario;
        String senhaTemporaria = null;
        Optional<Usuario> existente = usuarios.buscarPorEmail(requisicao.email().trim());
        if (existente.isPresent()) {
            usuario = existente.get();
            if (!usuario.isAtivo()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuário está inativo no sistema.");
            }
            if (vinculos.existsById(new UsuarioInstituicaoId(usuario.getId(), cnpj))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Este usuário já está vinculado à instituição (se estiver inativo, reative-o).");
            }
        } else {
            senhaTemporaria = gerarSenhaTemporaria();
            usuario = new Usuario();
            usuario.setNome(requisicao.nome().trim());
            usuario.setEmail(requisicao.email().trim());
            usuario.setSenhaHash(encoder.encode(senhaTemporaria));
            usuario.setDeveTrocarSenha(true);
            usuarios.save(usuario);
        }

        if (tipo != null) {
            garantirProfissional(usuario, tipo, requisicao.numeroConselho(), requisicao.ufConselho(), requisicao.especialidade());
        }

        UsuarioInstituicao vinculo = new UsuarioInstituicao();
        vinculo.setId(new UsuarioInstituicaoId(usuario.getId(), cnpj));
        vinculo.setUsuario(usuario);
        vinculo.setInstituicao(instituicao);
        vinculo.setDepartamento(departamento);
        vinculo.setAtivo(true);
        em.persist(vinculo);

        for (Papel papel : papeisDesejados) {
            em.persist(new UsuarioPapel(new UsuarioPapelId(usuario.getId(), cnpj, papel)));
        }

        AuditoriaContexto.adicionar("idUsuario", usuario.getId());
        return new UsuarioCriadoDto(montarDto(vinculo), senhaTemporaria);
    }

    // ------------------------------------------------------------ alterar papeis

    @Transactional
    public UsuarioInstituicaoDto alterarPapeis(String cnpjParam, Long idUsuario,
                                               UsuarioPapeisRequest requisicao, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirAdminDaInstituicao(solicitante, cnpj);
        carregarInstituicao(cnpj);
        UsuarioInstituicao vinculo = carregarVinculo(cnpj, idUsuario);
        if (!vinculo.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reative o usuário antes de alterar as permissões.");
        }

        Set<Papel> desejados = new HashSet<>(requisicao.papeis());
        TipoProfissional tipo = tipoProfissional(desejados);
        if (idUsuario.equals(solicitante) && !desejados.contains(Papel.ADMIN_INSTITUICAO)
                && !permissao.isAdminSistema(solicitante)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você não pode remover o seu próprio perfil de administrador.");
        }
        Departamento departamento = departamentoDaInstituicao(cnpj, requisicao.departamentoId());

        List<UsuarioPapel> atuais = papeis.listarPorUsuario(idUsuario).stream()
                .filter(p -> p.getId().getCnpjInstituicao().equals(cnpj))
                .toList();
        Set<Papel> atuaisSet = new HashSet<>();
        for (UsuarioPapel p : atuais) {
            atuaisSet.add(p.getId().getPapel());
        }

        papeis.deleteAll(atuais.stream().filter(p -> !desejados.contains(p.getId().getPapel())).toList());
        for (Papel papel : desejados) {
            if (!atuaisSet.contains(papel)) {
                em.persist(new UsuarioPapel(new UsuarioPapelId(idUsuario, cnpj, papel)));
            }
        }

        if (tipo != null) {
            garantirProfissional(vinculo.getUsuario(), tipo,
                    requisicao.numeroConselho(), requisicao.ufConselho(), requisicao.especialidade());
        }
        vinculo.setDepartamento(departamento);

        return montarDto(vinculo);
    }

    // ------------------------------------------------------- inativar / reativar

    /** "Remover" usuario e inativar o vinculo com a instituicao: o historico clinico continua referenciando-o. */
    @Transactional
    public UsuarioInstituicaoDto alterarAtivo(String cnpjParam, Long idUsuario, boolean ativo, Long solicitante) {
        String cnpj = Cnpj.normalizar(cnpjParam);
        permissao.exigirAdminDaInstituicao(solicitante, cnpj);
        Instituicao instituicao = carregarInstituicao(cnpj);
        UsuarioInstituicao vinculo = carregarVinculo(cnpj, idUsuario);

        if (!ativo && idUsuario.equals(solicitante)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você não pode inativar o seu próprio vínculo.");
        }
        if (ativo && !instituicao.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A instituição está inativa.");
        }

        vinculo.setAtivo(ativo);
        return montarDto(vinculo);
    }

    // ----------------------------------------------------------------- ajudantes

    private Instituicao carregarInstituicao(String cnpj) {
        return instituicoes.findById(cnpj)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instituição não encontrada."));
    }

    private UsuarioInstituicao carregarVinculo(String cnpj, Long idUsuario) {
        return vinculos.findById(new UsuarioInstituicaoId(idUsuario, cnpj))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuário não vinculado a esta instituição."));
    }

    /** Departamento opcional; se informado precisa ser ativo e da mesma instituicao. */
    private Departamento departamentoDaInstituicao(String cnpj, Long idDepartamento) {
        if (idDepartamento == null) {
            return null;
        }
        Departamento departamento = departamentos.findById(idDepartamento)
                .filter(d -> d.getInstituicao().getCnpj().equals(cnpj))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Departamento inválido para esta instituição."));
        if (!departamento.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O departamento está inativo.");
        }
        return departamento;
    }

    /** Medico e enfermeiro tem registro no conselho; um usuario so pode ter um desses dois perfis por vez. */
    private TipoProfissional tipoProfissional(Set<Papel> papeisDesejados) {
        boolean medico = papeisDesejados.contains(Papel.MEDICO);
        boolean enfermeiro = papeisDesejados.contains(Papel.ENFERMEIRO);
        if (medico && enfermeiro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Escolha apenas um perfil profissional: médico ou enfermeiro.");
        }
        if (medico) {
            return TipoProfissional.MEDICO;
        }
        return enfermeiro ? TipoProfissional.ENFERMEIRO : null;
    }

    /** Cria o registro profissional se ainda nao existe (exige numero e UF do conselho). */
    private void garantirProfissional(Usuario usuario, TipoProfissional tipo,
                                      String numeroConselho, String ufConselho, String especialidade) {
        ProfissionalSaudeId id = new ProfissionalSaudeId(usuario.getId(), tipo);
        if (profissionais.existsById(id)) {
            return;
        }
        if (numeroConselho == null || numeroConselho.isBlank()
                || ufConselho == null || !ufConselho.trim().matches("[A-Za-z]{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o número do conselho profissional e a UF (2 letras).");
        }
        ProfissionalSaude profissional = new ProfissionalSaude();
        profissional.setId(id);
        profissional.setUsuario(usuario);
        profissional.setNumeroConselho(numeroConselho.trim());
        profissional.setUfConselho(ufConselho.trim().toUpperCase());
        profissional.setEspecialidade(especialidade == null || especialidade.isBlank() ? null : especialidade.trim());
        em.persist(profissional);
    }

    private UsuarioInstituicaoDto montarDto(UsuarioInstituicao vinculo) {
        Long idUsuario = vinculo.getId().getIdUsuario();
        String cnpj = vinculo.getId().getCnpjInstituicao();
        List<String> papeisDoUsuario = ordenar(papeis.listarPorUsuario(idUsuario).stream()
                .filter(p -> p.getId().getCnpjInstituicao().equals(cnpj))
                .map(p -> p.getId().getPapel().name())
                .toList());
        ProfissionalSaude profissional = escolherProfissional(profissionais.listarPorUsuario(idUsuario), papeisDoUsuario);
        return paraDto(vinculo, papeisDoUsuario, profissional);
    }

    /** O registro profissional que corresponde ao papel exercido na instituicao. */
    private static ProfissionalSaude escolherProfissional(List<ProfissionalSaude> candidatos, List<String> papeisDoUsuario) {
        if (candidatos == null) {
            return null;
        }
        for (ProfissionalSaude p : candidatos) {
            if (papeisDoUsuario.contains(p.getId().getTipo().name())) {
                return p;
            }
        }
        return null;
    }

    private static List<String> ordenar(List<String> valores) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream().sorted().toList();
    }

    private static UsuarioInstituicaoDto paraDto(UsuarioInstituicao vinculo, List<String> papeisDoUsuario,
                                                 ProfissionalSaude profissional) {
        Usuario usuario = vinculo.getUsuario();
        Departamento departamento = vinculo.getDepartamento();
        return new UsuarioInstituicaoDto(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                vinculo.isAtivo(),
                papeisDoUsuario,
                departamento != null ? departamento.getId() : null,
                departamento != null ? departamento.getNome() : null,
                profissional != null ? profissional.getNumeroConselho() : null,
                profissional != null ? profissional.getUfConselho() : null,
                profissional != null ? profissional.getEspecialidade() : null);
    }

    private static String gerarSenhaTemporaria() {
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            senha.append(ALFABETO_SENHA.charAt(ALEATORIO.nextInt(ALFABETO_SENHA.length())));
        }
        return senha.toString();
    }
}
