package com.example.sistema_prontuarios.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * Um registro da linha do tempo do prontuario. "tipo" diz quais campos vem preenchidos:
 * TRIAGEM, DADO_CLINICO, CONDICAO_DIAGNOSTICO, PRESCRICAO_MEDICAMENTO ou REQUISICAO_EXAME.
 * Os demais campos vem nulos.
 */
public record RegistroClinicoDto(
        Long id,
        String tipo,
        Instant dataRegistro,
        String autor,
        String tipoAutor,
        boolean visivelAoPaciente,
        // TRIAGEM
        String classificacaoRisco,
        String sintomas,
        // DADO_CLINICO
        String tipoDado,
        String valor,
        // CONDICAO_DIAGNOSTICO
        String descricao,
        String cid,
        // PRESCRICAO_MEDICAMENTO
        String medicamento,
        String dosagem,
        String via,
        String frequencia,
        // REQUISICAO_EXAME ("statusExame": PENDENTE ate haver resultado, depois REALIZADO)
        String tipoExame,
        String statusExame,
        List<ResultadoExameDto> resultados) {
}
