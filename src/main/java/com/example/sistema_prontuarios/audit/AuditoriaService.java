package com.example.sistema_prontuarios.audit;

import com.example.sistema_prontuarios.repository.PacienteRepository;
import com.example.sistema_prontuarios.service.PermissaoService;
import com.example.sistema_prontuarios.util.Cnpj;
import com.example.sistema_prontuarios.util.Cpf;
import com.example.sistema_prontuarios.web.dto.AcessoAoPacienteDto;
import com.example.sistema_prontuarios.web.dto.LogAuditoriaDto;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Grava e consulta log_auditoria. A gravacao usa JDBC direto porque a coluna chave_recurso e jsonb,
 * e cada registro e confirmado na hora, em transacao propria.
 */
@Service
public class AuditoriaService {

    private final JdbcTemplate jdbc;
    private final PermissaoService permissao;
    private final PacienteRepository pacientes;

    public AuditoriaService(DataSource dataSource, PermissaoService permissao, PacienteRepository pacientes) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.permissao = permissao;
        this.pacientes = pacientes;
    }

    /** "idUsuario" e nulo quando nao ha usuario identificado (ex.: tentativa de login). */
    public void registrar(Long idUsuario, String acao, String tipoRecurso, Map<String, Object> chaveRecurso) {
        jdbc.update(
                "insert into log_auditoria (id_usuario, acao, tipo_recurso, chave_recurso) "
                        + "values (?, ?, ?, cast(? as jsonb))",
                new Object[] {idUsuario, acao, tipoRecurso, Json.escrever(chaveRecurso)},
                new int[] {Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
    }

    /**
     * Consulta do log. Sem cnpj: todas as instituicoes (somente admin do sistema).
     * Com cnpj: so os eventos daquela instituicao (admin do sistema ou admin dela).
     */
    @Transactional(readOnly = true)
    public List<LogAuditoriaDto> consultar(Long solicitante, String cnpjParam, Long idUsuario, String acao,
                                           String cpfParam, Instant de, Instant ate, Integer limite) {
        String cnpj = cnpjParam == null || cnpjParam.isBlank() ? null : Cnpj.normalizar(cnpjParam);
        if (cnpj == null) {
            permissao.exigirAdminSistema(solicitante);
        } else {
            permissao.exigirAdminDaInstituicao(solicitante, cnpj);
        }

        StringBuilder sql = new StringBuilder(
                "select l.id_log, l.instante, l.id_usuario, u.nome, l.acao, l.tipo_recurso, l.chave_recurso::text "
                        + "from log_auditoria l left join usuario u on u.id_usuario = l.id_usuario where 1 = 1");
        List<Object> argumentos = new ArrayList<>();
        List<Integer> tipos = new ArrayList<>();

        if (cnpj != null) {
            sql.append(" and l.chave_recurso ->> 'cnpj' = ?");
            argumentos.add(cnpj);
            tipos.add(Types.VARCHAR);
        }
        if (idUsuario != null) {
            sql.append(" and l.id_usuario = ?");
            argumentos.add(idUsuario);
            tipos.add(Types.BIGINT);
        }
        if (acao != null && !acao.isBlank()) {
            sql.append(" and l.acao = ?");
            argumentos.add(acao.trim().toUpperCase());
            tipos.add(Types.VARCHAR);
        }
        if (cpfParam != null && !cpfParam.isBlank()) {
            sql.append(" and l.chave_recurso ->> 'cpf' = ?");
            argumentos.add(Cpf.normalizar(cpfParam));
            tipos.add(Types.VARCHAR);
        }
        if (de != null) {
            sql.append(" and l.instante >= ?");
            argumentos.add(Timestamp.from(de));
            tipos.add(Types.TIMESTAMP);
        }
        if (ate != null) {
            sql.append(" and l.instante <= ?");
            argumentos.add(Timestamp.from(ate));
            tipos.add(Types.TIMESTAMP);
        }
        int maximo = limite == null ? 100 : Math.max(1, Math.min(limite, 500));
        sql.append(" order by l.instante desc, l.id_log desc limit ").append(maximo);

        return jdbc.query(sql.toString(), argumentos.toArray(), tipos.stream().mapToInt(Integer::intValue).toArray(),
                (rs, linha) -> new LogAuditoriaDto(
                        rs.getLong(1),
                        rs.getTimestamp(2).toInstant(),
                        rs.getObject(3) == null ? null : rs.getLong(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7)));
    }

    /** O paciente ve o que outras pessoas fizeram com os seus dados (as ultimas 100 acoes, inclusive as negadas). */
    @Transactional(readOnly = true)
    public List<AcessoAoPacienteDto> acessosAoPaciente(Long idUsuario) {
        permissao.exigirUsuarioApto(idUsuario);
        String cpf = pacientes.buscarPorUsuario(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito a pacientes."))
                .getCpf();

        return jdbc.query(
                "select l.instante, l.acao, u.nome, i.nome, "
                        + "coalesce((l.chave_recurso ->> 'status')::int, 0), "
                        + "coalesce((l.chave_recurso ->> 'entreInstituicoes')::boolean, false) "
                        + "from log_auditoria l "
                        + "left join usuario u on u.id_usuario = l.id_usuario "
                        + "left join instituicao i on i.cnpj = (l.chave_recurso ->> 'cnpj') "
                        + "where l.chave_recurso ->> 'cpf' = ? and l.id_usuario is distinct from ? "
                        + "order by l.instante desc, l.id_log desc limit 100",
                new Object[] {cpf, idUsuario},
                new int[] {Types.VARCHAR, Types.BIGINT},
                (rs, linha) -> new AcessoAoPacienteDto(
                        rs.getTimestamp(1).toInstant(),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getInt(5),
                        rs.getBoolean(6)));
    }
}
