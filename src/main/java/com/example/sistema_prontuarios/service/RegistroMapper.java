package com.example.sistema_prontuarios.service;

import com.example.sistema_prontuarios.domain.CondicaoDiagnostico;
import com.example.sistema_prontuarios.domain.DadoClinico;
import com.example.sistema_prontuarios.domain.PrescricaoMedicamento;
import com.example.sistema_prontuarios.domain.RegistroClinico;
import com.example.sistema_prontuarios.domain.RequisicaoExame;
import com.example.sistema_prontuarios.domain.ResultadoExame;
import com.example.sistema_prontuarios.domain.Triagem;
import com.example.sistema_prontuarios.web.dto.RegistroClinicoDto;
import com.example.sistema_prontuarios.web.dto.ResultadoExameDto;
import java.util.List;

/** Converte os registros clinicos (cada subtipo tem campos proprios) para o formato unico da linha do tempo. */
final class RegistroMapper {

    private RegistroMapper() {
    }

    /** "resultados" so importa para requisicao de exame; para os outros tipos pode vir vazio. */
    static RegistroClinicoDto paraDto(RegistroClinico r, List<ResultadoExame> resultados) {
        String tipo;
        String classificacaoRisco = null;
        String sintomas = null;
        String tipoDado = null;
        String valor = null;
        String descricao = null;
        String cid = null;
        String medicamento = null;
        String dosagem = null;
        String via = null;
        String frequencia = null;
        String tipoExame = null;
        String statusExame = null;
        List<ResultadoExameDto> resultadosDto = null;

        if (r instanceof Triagem t) {
            tipo = "TRIAGEM";
            classificacaoRisco = t.getClassificacaoRisco();
            sintomas = t.getSintomas();
        } else if (r instanceof DadoClinico d) {
            tipo = "DADO_CLINICO";
            tipoDado = d.getTipoDado();
            valor = d.getValor();
        } else if (r instanceof CondicaoDiagnostico c) {
            tipo = "CONDICAO_DIAGNOSTICO";
            descricao = c.getDescricao();
            cid = c.getCid();
        } else if (r instanceof PrescricaoMedicamento p) {
            tipo = "PRESCRICAO_MEDICAMENTO";
            medicamento = p.getMedicamento();
            dosagem = p.getDosagem();
            via = p.getVia();
            frequencia = p.getFrequencia();
        } else if (r instanceof RequisicaoExame e) {
            tipo = "REQUISICAO_EXAME";
            tipoExame = e.getTipoExame();
            resultadosDto = resultados.stream().map(RegistroMapper::resultadoParaDto).toList();
            statusExame = resultadosDto.isEmpty() ? "PENDENTE" : "REALIZADO";
        } else {
            tipo = "DESCONHECIDO";
        }

        return new RegistroClinicoDto(
                r.getId(), tipo, r.getDataRegistro(),
                r.getAutor().getUsuario().getNome(), r.getAutor().getId().getTipo().name(),
                r.isVisivelAoPaciente(),
                classificacaoRisco, sintomas,
                tipoDado, valor,
                descricao, cid,
                medicamento, dosagem, via, frequencia,
                tipoExame, statusExame, resultadosDto);
    }

    static ResultadoExameDto resultadoParaDto(ResultadoExame x) {
        return new ResultadoExameDto(
                x.getId().getVersao(),
                x.getResultado(),
                x.getDataResultado(),
                x.getRegistrador().getUsuario().getNome(),
                x.getRegistrador().getId().getTipo().name());
    }
}
