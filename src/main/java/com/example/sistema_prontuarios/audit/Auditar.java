package com.example.sistema_prontuarios.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um endpoint para auditoria: cada chamada gera um registro em log_auditoria com quem chamou,
 * a acao, o recurso, as variaveis da URL, o status HTTP da resposta e o IP. Nunca grava o corpo da requisicao.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditar {

    /** Nome da acao, ex.: CONSULTAR_PRONTUARIO. */
    String acao();

    /** Tipo do recurso, ex.: ATENDIMENTO. */
    String recurso();
}
