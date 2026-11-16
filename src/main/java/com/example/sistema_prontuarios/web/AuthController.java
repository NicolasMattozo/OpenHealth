package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.service.AuthService;
import com.example.sistema_prontuarios.web.dto.LoginRequest;
import com.example.sistema_prontuarios.web.dto.LoginResponse;
import com.example.sistema_prontuarios.web.dto.TrocarSenhaRequest;
import com.example.sistema_prontuarios.web.dto.UsuarioLogadoDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.sistema_prontuarios.audit.Auditar;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Auditar(acao = "LOGIN", recurso = "USUARIO")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return authService.login(requisicao.identificador(), requisicao.senha());
    }

    /** Dados atualizados do usuario dono do token. */
    @GetMapping("/me")
    public UsuarioLogadoDto me(@AuthenticationPrincipal Jwt jwt) {
        return authService.usuarioLogado(Long.valueOf(jwt.getSubject()));
    }

    /** Obrigatorio no primeiro acesso (senha temporaria). Devolve um token novo, ja com os papeis. */
    @Auditar(acao = "TROCAR_SENHA", recurso = "USUARIO")
    @PostMapping("/trocar-senha")
    public LoginResponse trocarSenha(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TrocarSenhaRequest requisicao) {
        return authService.trocarSenha(Long.valueOf(jwt.getSubject()), requisicao.senhaAtual(), requisicao.novaSenha());
    }
}
