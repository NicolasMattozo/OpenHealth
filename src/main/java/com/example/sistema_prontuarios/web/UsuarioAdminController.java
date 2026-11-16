package com.example.sistema_prontuarios.web;

import com.example.sistema_prontuarios.service.UsuarioAdminService;
import com.example.sistema_prontuarios.web.dto.UsuarioCadastroRequest;
import com.example.sistema_prontuarios.web.dto.UsuarioCriadoDto;
import com.example.sistema_prontuarios.web.dto.UsuarioInstituicaoDto;
import com.example.sistema_prontuarios.web.dto.UsuarioPapeisRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.example.sistema_prontuarios.audit.Auditar;

/** Usuarios de uma instituicao. O escopo (so a propria instituicao) e conferido no servico. */
@RestController
@RequestMapping("/api/instituicoes/{cnpj}/usuarios")
@PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'ADMIN_INSTITUICAO')")
public class UsuarioAdminController {

    private final UsuarioAdminService service;

    public UsuarioAdminController(UsuarioAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioInstituicaoDto> listar(@PathVariable String cnpj, @AuthenticationPrincipal Jwt jwt) {
        return service.listar(cnpj, idDe(jwt));
    }

    @Auditar(acao = "CADASTRAR_USUARIO", recurso = "USUARIO")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioCriadoDto cadastrar(@PathVariable String cnpj, @Valid @RequestBody UsuarioCadastroRequest requisicao,
                                      @AuthenticationPrincipal Jwt jwt) {
        return service.cadastrar(cnpj, requisicao, idDe(jwt));
    }

    @Auditar(acao = "ALTERAR_PAPEIS", recurso = "USUARIO")
    @PutMapping("/{idUsuario}/papeis")
    public UsuarioInstituicaoDto alterarPapeis(@PathVariable String cnpj, @PathVariable Long idUsuario,
                                               @Valid @RequestBody UsuarioPapeisRequest requisicao,
                                               @AuthenticationPrincipal Jwt jwt) {
        return service.alterarPapeis(cnpj, idUsuario, requisicao, idDe(jwt));
    }

    /** Remover = inativar o vinculo do usuario com a instituicao. */
    @Auditar(acao = "INATIVAR_USUARIO", recurso = "USUARIO")
    @DeleteMapping("/{idUsuario}")
    public UsuarioInstituicaoDto inativar(@PathVariable String cnpj, @PathVariable Long idUsuario,
                                          @AuthenticationPrincipal Jwt jwt) {
        return service.alterarAtivo(cnpj, idUsuario, false, idDe(jwt));
    }

    @Auditar(acao = "REATIVAR_USUARIO", recurso = "USUARIO")
    @PostMapping("/{idUsuario}/reativar")
    public UsuarioInstituicaoDto reativar(@PathVariable String cnpj, @PathVariable Long idUsuario,
                                          @AuthenticationPrincipal Jwt jwt) {
        return service.alterarAtivo(cnpj, idUsuario, true, idDe(jwt));
    }

    private static Long idDe(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
