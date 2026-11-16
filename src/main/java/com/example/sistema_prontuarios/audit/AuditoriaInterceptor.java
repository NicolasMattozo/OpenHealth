package com.example.sistema_prontuarios.audit;

import com.example.sistema_prontuarios.util.Cnpj;
import com.example.sistema_prontuarios.util.Cpf;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Grava o registro de auditoria dos endpoints marcados com @Auditar, depois que a resposta foi decidida
 * (assim o status HTTP, inclusive 403 e 404, entra no registro). Se a gravacao falhar, so loga o erro.
 */
@Component
public class AuditoriaInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaInterceptor.class);

    private final AuditoriaService auditoria;

    public AuditoriaInterceptor(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public void afterCompletion(HttpServletRequest requisicao, HttpServletResponse resposta,
                                Object handler, Exception excecao) {
        if (!(handler instanceof HandlerMethod metodo)) {
            return;
        }
        Auditar auditar = metodo.getMethodAnnotation(Auditar.class);
        if (auditar == null) {
            return;
        }
        try {
            Map<String, Object> chave = new LinkedHashMap<>();
            Object variaveis = requisicao.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (variaveis instanceof Map<?, ?> mapa) {
                for (Map.Entry<?, ?> variavel : mapa.entrySet()) {
                    String nome = String.valueOf(variavel.getKey());
                    chave.put(nome, valorDaUrl(nome, String.valueOf(variavel.getValue())));
                }
            }
            chave.putAll(AuditoriaContexto.dados(requisicao));
            chave.put("status", status(resposta, excecao));
            chave.put("ip", requisicao.getRemoteAddr());

            auditoria.registrar(usuario(requisicao), auditar.acao(), auditar.recurso(), chave);
        } catch (RuntimeException erro) {
            log.error("Falha ao gravar auditoria da acao {}: {}", auditar.acao(), erro.getMessage());
        }
    }

    /** Padroniza cnpj e cpf (so os caracteres validos) e guarda ids como numero. */
    private static Object valorDaUrl(String nome, String valor) {
        if (nome.equals("cnpj")) {
            return Cnpj.normalizar(valor);
        }
        if (nome.equals("cpf")) {
            return Cpf.normalizar(valor);
        }
        if (nome.startsWith("id") && valor.matches("\\d{1,18}")) {
            return Long.valueOf(valor);
        }
        return valor;
    }

    private static int status(HttpServletResponse resposta, Exception excecao) {
        if (excecao instanceof AccessDeniedException) {
            return 403;
        }
        return excecao != null ? 500 : resposta.getStatus();
    }

    private static Long usuario(HttpServletRequest requisicao) {
        Long definido = AuditoriaContexto.usuario(requisicao);
        if (definido != null) {
            return definido;
        }
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken token) {
            try {
                return Long.valueOf(token.getToken().getSubject());
            } catch (NumberFormatException erro) {
                return null;
            }
        }
        return null;
    }
}
