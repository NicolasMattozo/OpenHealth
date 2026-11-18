package com.example.sistema_prontuarios.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TentativasLoginServiceTest {

    /** Servico com relogio manual: da para "esperar 15 minutos" sem esperar. */
    static class ComRelogio extends TentativasLoginService {
        Instant agora = Instant.parse("2026-11-17T12:00:00Z");

        @Override
        protected Instant agora() {
            return agora;
        }

        void avancar(Duration d) {
            agora = agora.plus(d);
        }
    }

    private static void falhar(TentativasLoginService s, String id, int vezes) {
        for (int i = 0; i < vezes; i++) {
            s.registrarFalha(id);
        }
    }

    @Test
    void quatroFalhasNaoBloqueiamACincoBloqueia() {
        ComRelogio s = new ComRelogio();
        falhar(s, "ana@x.com", 4);
        assertFalse(s.estaBloqueado("ana@x.com"));
        s.registrarFalha("ana@x.com");
        assertTrue(s.estaBloqueado("ana@x.com"));
        assertEquals(15, s.minutosRestantes("ana@x.com"));
    }

    @Test
    void desbloqueiaSozinhoAposQuinzeMinutos() {
        ComRelogio s = new ComRelogio();
        falhar(s, "ana@x.com", 5);
        s.avancar(Duration.ofMinutes(10));
        assertTrue(s.estaBloqueado("ana@x.com"));
        assertEquals(5, s.minutosRestantes("ana@x.com"));
        s.avancar(Duration.ofMinutes(5));
        assertFalse(s.estaBloqueado("ana@x.com"));
        s.registrarFalha("ana@x.com");
        assertFalse(s.estaBloqueado("ana@x.com"), "depois de liberado comeca um ciclo novo");
    }

    @Test
    void emailECpfSaoNormalizados() {
        ComRelogio s = new ComRelogio();
        s.registrarFalha("Ana@X.com ");
        s.registrarFalha("ana@x.com");
        s.registrarFalha(" ANA@x.COM");
        s.registrarFalha("ana@x.com");
        s.registrarFalha("ana@X.com");
        assertTrue(s.estaBloqueado("ana@x.com"));

        s.registrarFalha("529.982.247-25");
        s.registrarFalha("52998224725");
        s.registrarFalha("529982247-25");
        s.registrarFalha("529.982.24725");
        s.registrarFalha("52998224725");
        assertTrue(s.estaBloqueado("529.982.247-25"));
        assertTrue(s.estaBloqueado("52998224725"));
    }

    @Test
    void loginComSucessoZeraOContador() {
        ComRelogio s = new ComRelogio();
        falhar(s, "ze@x.com", 4);
        s.limpar("zE@x.com");
        s.registrarFalha("ze@x.com");
        assertFalse(s.estaBloqueado("ze@x.com"));
    }

    @Test
    void falhasAntigasNaoContam() {
        ComRelogio s = new ComRelogio();
        falhar(s, "jo@x.com", 4);
        s.avancar(Duration.ofMinutes(20));
        s.registrarFalha("jo@x.com");
        assertFalse(s.estaBloqueado("jo@x.com"));
    }

    @Test
    void falhasDentroDaJanelaContinuamSomando() {
        ComRelogio s = new ComRelogio();
        for (int i = 0; i < 4; i++) {
            s.registrarFalha("ka@x.com");
            s.avancar(Duration.ofMinutes(3));
        }
        s.registrarFalha("ka@x.com");
        assertTrue(s.estaBloqueado("ka@x.com"));
    }

    @Test
    void novasFalhasNaoEstendemOBloqueio() {
        ComRelogio s = new ComRelogio();
        falhar(s, "ed@x.com", 5);
        s.avancar(Duration.ofMinutes(10));
        s.registrarFalha("ed@x.com");
        assertEquals(5, s.minutosRestantes("ed@x.com"));
    }

    @Test
    void identificadoresSaoIndependentes() {
        ComRelogio s = new ComRelogio();
        falhar(s, "a@x.com", 5);
        assertTrue(s.estaBloqueado("a@x.com"));
        assertFalse(s.estaBloqueado("b@x.com"));
    }

    @Test
    void liberaMemoriaDeQuemErrouUmaVezENuncaMaisVoltou() {
        ComRelogio s = new ComRelogio();
        for (int i = 0; i < 1000; i++) {
            s.registrarFalha("fantasma" + i + "@x.com");
        }
        assertEquals(1000, s.tamanho());
        s.avancar(Duration.ofMinutes(20));
        s.registrarFalha("novo@x.com");
        assertEquals(1, s.tamanho());
    }

    @Test
    void falhasSimultaneasNaoQuebramOContador() throws InterruptedException {
        ComRelogio s = new ComRelogio();
        Thread[] threads = new Thread[200];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> s.registrarFalha("conc@x.com"));
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assertTrue(s.estaBloqueado("conc@x.com"));
        assertEquals(1, s.tamanho());
    }
}
