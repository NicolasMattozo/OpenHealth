package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.Paciente;
import com.example.sistema_prontuarios.domain.Usuario;
import com.example.sistema_prontuarios.domain.enums.Papel;
import com.example.sistema_prontuarios.repository.PacienteRepository;
import com.example.sistema_prontuarios.repository.UsuarioRepository;
import com.example.sistema_prontuarios.util.Cnpj;
import com.example.sistema_prontuarios.util.Cpf;
import com.example.sistema_prontuarios.web.dto.PacienteAtualizacaoRequest;
import com.example.sistema_prontuarios.web.dto.PacienteCriadoDto;
import com.example.sistema_prontuarios.web.dto.PacienteDto;
import com.example.sistema_prontuarios.web.dto.PacienteRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.sistema_prontuarios.audit.AuditoriaContexto;

/**
 * Casos de uso 2 e 3: cadastrar e consultar o cadastro do paciente (somente dados nao clinicos).
 * O cadastro de pacientes e unico para toda a rede; a instituicao so serve para checar a permissao da recepcao.
 */
@Service
public class PacienteService {

    private static final String ALFABETO_SENHA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final PacienteRepository pacientes;
    private final UsuarioRepository usuarios;
    private final PermissaoService permissao;
    private final PasswordEncoder encoder;

    @PersistenceContext
    private EntityManager em;

    public PacienteService(PacienteRepository pacientes,
                           UsuarioRepository usuarios,
                           PermissaoService permissao,
                           PasswordEncoder encoder) {
        this.pacientes = pacientes;
        this.usuarios = usuarios;
        this.permissao = permissao;
        this.encoder = encoder;
    }

    @Transactional
    public PacienteCriadoDto cadastrar(String cnpjParam, PacienteRequest requisicao, Long solicitante) {
        permissao.exigirPapel(solicitante, Cnpj.normalizar(cnpjParam), Papel.RECEPCIONISTA);

        String cpf = Cpf.normalizar(requisicao.cpf());
        AuditoriaContexto.adicionar("cpf", cpf);
        if (!Cpf.valido(cpf)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF inválido.");
        }
        var existente = pacientes.findById(cpf);
        if (existente.isPresent()) {
            throw new PacienteJaCadastradoException(paraDto(existente.get()));
        }
        String email = requisicao.email().trim();
        if (usuarios.buscarPorEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está em uso por outro usuário.");
        }

        String senhaTemporaria = gerarSenhaTemporaria();
        Usuario usuario = new Usuario();
        usuario.setNome(requisicao.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(encoder.encode(senhaTemporaria));
        usuario.setDeveTrocarSenha(true);
        usuarios.save(usuario);

        Paciente paciente = new Paciente();
        paciente.setCpf(cpf);
        paciente.setUsuario(usuario);
        paciente.setDataNascimento(requisicao.dataNascimento());
        paciente.setTelefone(vazioParaNulo(requisicao.telefone()));
        paciente.setEndereco(vazioParaNulo(requisicao.endereco()));
        em.persist(paciente);

        return new PacienteCriadoDto(paraDto(paciente), senhaTemporaria);
    }

    /** Busca por CPF completo ou por parte do nome (minimo 3 caracteres), no maximo 20 resultados. */
    @Transactional(readOnly = true)
    public List<PacienteDto> buscar(String cnpjParam, String busca, Long solicitante) {
        permissao.exigirPapel(solicitante, Cnpj.normalizar(cnpjParam), Papel.RECEPCIONISTA);

        String texto = busca == null ? "" : busca.trim();
        if (texto.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o CPF ou pelo menos 3 letras do nome.");
        }
        String digitos = Cpf.normalizar(texto);
        boolean pareceCpf = texto.matches("[0-9.\\-\\s]+") && digitos.length() == 11;
        String cpf = pareceCpf ? digitos : "";

        return pacientes.buscar(cpf, escaparLike(texto), PageRequest.of(0, 20)).stream()
                .map(PacienteService::paraDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PacienteDto consultar(String cnpjParam, String cpfParam, Long solicitante) {
        permissao.exigirPapel(solicitante, Cnpj.normalizar(cnpjParam), Papel.RECEPCIONISTA);
        return paraDto(carregar(cpfParam));
    }

    @Transactional
    public PacienteDto atualizarContato(String cnpjParam, String cpfParam,
                                        PacienteAtualizacaoRequest requisicao, Long solicitante) {
        permissao.exigirPapel(solicitante, Cnpj.normalizar(cnpjParam), Papel.RECEPCIONISTA);
        Paciente paciente = carregar(cpfParam);
        paciente.setTelefone(vazioParaNulo(requisicao.telefone()));
        paciente.setEndereco(vazioParaNulo(requisicao.endereco()));
        return paraDto(paciente);
    }

    private Paciente carregar(String cpfParam) {
        return pacientes.findById(Cpf.normalizar(cpfParam))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente não encontrado."));
    }

    /** Protege o LIKE: o texto digitado nao pode virar curinga. */
    private static String escaparLike(String texto) {
        return texto.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String gerarSenhaTemporaria() {
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            senha.append(ALFABETO_SENHA.charAt(ALEATORIO.nextInt(ALFABETO_SENHA.length())));
        }
        return senha.toString();
    }

    static PacienteDto paraDto(Paciente p) {
        Usuario u = p.getUsuario();
        return new PacienteDto(p.getCpf(), u.getNome(), u.getEmail(), p.getDataNascimento(),
                p.getTelefone(), p.getEndereco());
    }
}
