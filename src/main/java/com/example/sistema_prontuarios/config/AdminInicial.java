package com.example.sistema_prontuarios.config;

import com.example.sistema_prontuarios.domain.Usuario;
import com.example.sistema_prontuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Cria o administrador do sistema na primeira execucao, para ser possivel fazer o primeiro login. */
@Component
public class AdminInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String email;
    private final String nome;
    private final String senha;

    public AdminInicial(UsuarioRepository usuarios,
                        PasswordEncoder encoder,
                        @Value("${app.admin.email}") String email,
                        @Value("${app.admin.nome}") String nome,
                        @Value("${app.admin.senha}") String senha) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.email = email;
        this.nome = nome;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        if (senha == null || senha.isBlank()) {
            log.warn("app.admin.senha vazia: administrador inicial nao foi criado.");
            return;
        }
        if (usuarios.buscarPorEmail(email).isPresent()) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setEmail(email);
        admin.setNome(nome);
        admin.setSenhaHash(encoder.encode(senha));
        admin.setAdministradorSistema(true);
        usuarios.save(admin);
        log.info("Administrador inicial criado: {}", email);
    }
}
