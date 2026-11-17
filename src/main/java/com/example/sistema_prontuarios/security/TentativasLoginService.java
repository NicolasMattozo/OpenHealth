package com.example.sistema_prontuarios.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Controla tentativas consecutivas de login com falha por e-mail ou CPF.
 * Bloqueia por 15 minutos apos 5 falhas consecutivas.
 */
@Service
public class TentativasLoginService {

    private static final int MAX_TENTATIVAS = 5;
    private static final Duration TEMPO_BLOQUEIO = Duration.ofMinutes(15);

    private static class RegistroTentativa {
        int tentativas = 0;
        Instant bloqueadoAte = null;
    }

    private final Map<String, RegistroTentativa> mapa = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String identificador) {
        String chave = normalizar(identificador);
        RegistroTentativa reg = mapa.get(chave);
        if (reg == null) {
            return false;
        }
        if (reg.bloqueadoAte != null) {
            if (Instant.now().isBefore(reg.bloqueadoAte)) {
                return true;
            }
            // Ja passou o tempo de bloqueio: libera
            mapa.remove(chave);
            return false;
        }
        return false;
    }

    public long minutosRestantes(String identificador) {
        String chave = normalizar(identificador);
        RegistroTentativa reg = mapa.get(chave);
        if (reg != null && reg.bloqueadoAte != null) {
            long segundos = Duration.between(Instant.now(), reg.bloqueadoAte).getSeconds();
            return Math.max(1, (segundos + 59) / 60);
        }
        return 0;
    }

    public void registrarFalha(String identificador) {
        String chave = normalizar(identificador);
        mapa.compute(chave, (k, reg) -> {
            if (reg == null) {
                reg = new RegistroTentativa();
            }
            // Se o bloqueio anterior expirou, reseta o ciclo
            if (reg.bloqueadoAte != null && Instant.now().isAfter(reg.bloqueadoAte)) {
                reg.tentativas = 0;
                reg.bloqueadoAte = null;
            }
            reg.tentativas++;
            if (reg.tentativas >= MAX_TENTATIVAS) {
                reg.bloqueadoAte = Instant.now().plus(TEMPO_BLOQUEIO);
            }
            return reg;
        });
    }

    public void limpar(String identificador) {
        mapa.remove(normalizar(identificador));
    }

    /** E-mail em minusculo; CPF apenas numeros. */
    private static String normalizar(String identificador) {
        if (identificador == null) return "";
        String limpo = identificador.trim();
        if (limpo.contains("@")) {
            return limpo.toLowerCase();
        }
        return limpo.replaceAll("\\D", "");
    }
}