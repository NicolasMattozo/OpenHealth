package com.example.sistema_prontuarios.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Limita tentativas de login com falha por e-mail ou CPF: na 5a falha dentro de 15 minutos o
 * identificador fica bloqueado por 15 minutos. O estado fica em memoria (zera ao reiniciar o servidor).
 *
 * Diferencas para a primeira versao:
 *  - janela deslizante: falhas com mais de 15 min deixam de contar (4 erros hoje + 1 erro na semana
 *    que vem nao bloqueiam ninguem);
 *  - limpeza periodica: identificadores que erraram uma vez e nunca mais voltaram nao ficam
 *    ocupando memoria para sempre;
 *  - registros imutaveis (record): leitura segura entre threads;
 *  - o relogio vem de agora(), o que permite testar sem esperar 15 minutos.
 */
@Service
public class TentativasLoginService {

    static final int MAX_TENTATIVAS = 5;
    static final Duration TEMPO_BLOQUEIO = Duration.ofMinutes(15);
    /** Falhas mais antigas que isto deixam de contar. */
    static final Duration JANELA = Duration.ofMinutes(15);
    private static final Duration INTERVALO_LIMPEZA = Duration.ofMinutes(5);

    private record Registro(int falhas, Instant ultimaFalha, Instant bloqueadoAte) {

        boolean bloqueioExpirou(Instant agora) {
            return bloqueadoAte != null && !agora.isBefore(bloqueadoAte);
        }

        boolean foraDaJanela(Instant agora) {
            return bloqueadoAte == null && agora.isAfter(ultimaFalha.plus(JANELA));
        }

        boolean descartavel(Instant agora) {
            return bloqueioExpirou(agora) || foraDaJanela(agora);
        }
    }

    private final Map<String, Registro> mapa = new ConcurrentHashMap<>();
    private volatile Instant proximaLimpeza = Instant.MIN;

    /** Relogio do servico; os testes sobrescrevem para simular a passagem do tempo. */
    protected Instant agora() {
        return Instant.now();
    }

    public boolean estaBloqueado(String identificador) {
        String chave = normalizar(identificador);
        Registro r = mapa.get(chave);
        if (r == null || r.bloqueadoAte() == null) {
            return false;
        }
        if (agora().isBefore(r.bloqueadoAte())) {
            return true;
        }
        mapa.remove(chave, r); // o bloqueio acabou: libera (so remove se ninguem alterou nesse meio tempo)
        return false;
    }

    public long minutosRestantes(String identificador) {
        Registro r = mapa.get(normalizar(identificador));
        if (r == null || r.bloqueadoAte() == null) {
            return 0;
        }
        long segundos = Duration.between(agora(), r.bloqueadoAte()).getSeconds();
        return Math.max(1, (segundos + 59) / 60);
    }

    public void registrarFalha(String identificador) {
        String chave = normalizar(identificador);
        Instant agora = agora();
        limparSeNecessario(agora);
        mapa.compute(chave, (k, r) -> {
            if (r != null && r.bloqueadoAte() != null && !r.bloqueioExpirou(agora)) {
                return r; // ja bloqueado: nao estende o bloqueio
            }
            int falhas = (r == null || r.descartavel(agora)) ? 1 : r.falhas() + 1;
            Instant bloqueadoAte = falhas >= MAX_TENTATIVAS ? agora.plus(TEMPO_BLOQUEIO) : null;
            return new Registro(falhas, agora, bloqueadoAte);
        });
    }

    public void limpar(String identificador) {
        mapa.remove(normalizar(identificador));
    }

    /** Quantos identificadores estao sendo acompanhados (usado nos testes). */
    int tamanho() {
        return mapa.size();
    }

    private void limparSeNecessario(Instant agora) {
        if (agora.isBefore(proximaLimpeza)) {
            return;
        }
        proximaLimpeza = agora.plus(INTERVALO_LIMPEZA);
        mapa.entrySet().removeIf(e -> e.getValue().descartavel(agora));
    }

    /** E-mail em minusculo; CPF apenas numeros. */
    private static String normalizar(String identificador) {
        if (identificador == null) {
            return "";
        }
        String limpo = identificador.trim();
        if (limpo.contains("@")) {
            return limpo.toLowerCase();
        }
        return limpo.replaceAll("\\D", "");
    }
}
