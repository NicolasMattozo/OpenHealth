package com.example.sistema_prontuarios.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Dados que os servicos acrescentam ao registro de auditoria da requisicao em andamento
 * (ex.: o CPF do paciente, que nem sempre aparece na URL). Fora de uma requisicao nao faz nada.
 */
public final class AuditoriaContexto {

    private static final String DADOS = AuditoriaContexto.class.getName() + ".dados";
    private static final String USUARIO = AuditoriaContexto.class.getName() + ".usuario";

    private AuditoriaContexto() {
    }

    /** Acrescenta (ou substitui) um campo da chave do recurso. Valor nulo e ignorado. */
    @SuppressWarnings("unchecked")
    public static void adicionar(String chave, Object valor) {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos == null || valor == null) {
            return;
        }
        Object atual = atributos.getAttribute(DADOS, RequestAttributes.SCOPE_REQUEST);
        Map<String, Object> dados;
        if (atual instanceof Map) {
            dados = (Map<String, Object>) atual;
        } else {
            dados = new LinkedHashMap<>();
            atributos.setAttribute(DADOS, dados, RequestAttributes.SCOPE_REQUEST);
        }
        dados.put(chave, valor);
    }

    /** Para quando o usuario so e conhecido durante a requisicao (login ainda nao tem token). */
    public static void definirUsuario(Long idUsuario) {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos != null && idUsuario != null) {
            atributos.setAttribute(USUARIO, idUsuario, RequestAttributes.SCOPE_REQUEST);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> dados(HttpServletRequest requisicao) {
        Object atual = requisicao.getAttribute(DADOS);
        return atual instanceof Map ? (Map<String, Object>) atual : Map.of();
    }

    static Long usuario(HttpServletRequest requisicao) {
        Object atual = requisicao.getAttribute(USUARIO);
        return atual instanceof Long id ? id : null;
    }
}
