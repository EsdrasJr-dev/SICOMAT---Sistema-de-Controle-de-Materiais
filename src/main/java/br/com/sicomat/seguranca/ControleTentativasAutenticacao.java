package br.com.sicomat.seguranca;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/** Aplica limite temporário de tentativas para uma única credencial. */
public final class ControleTentativasAutenticacao {

    private static final int MAXIMO_FALHAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(5);
    private static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(5);

    private final Deque<Instant> falhas = new ArrayDeque<>();
    private Instant bloqueadoAte;

    public synchronized void exigirTentativaPermitida() {
        Instant agora = Instant.now();
        if (bloqueadoAte != null) {
            if (agora.isBefore(bloqueadoAte)) {
                throw new IllegalStateException("Muitas tentativas inválidas. Tente novamente após " + bloqueadoAte + ".");
            }
            bloqueadoAte = null;
            falhas.clear();
        }
        removerFalhasAntigas(agora);
    }

    /** Retorna true quando esta falha inicia o bloqueio temporário. */
    public synchronized boolean registrarFalha() {
        exigirTentativaPermitida();
        Instant agora = Instant.now();
        removerFalhasAntigas(agora);
        falhas.addLast(agora);
        if (falhas.size() >= MAXIMO_FALHAS) {
            bloqueadoAte = agora.plus(DURACAO_BLOQUEIO);
            falhas.clear();
            return true;
        }
        return false;
    }

    public synchronized void registrarSucesso() {
        falhas.clear();
    }

    public synchronized void redefinir() {
        falhas.clear();
        bloqueadoAte = null;
    }

    private void removerFalhasAntigas(Instant agora) {
        Instant limite = agora.minus(JANELA);
        while (!falhas.isEmpty() && !falhas.peekFirst().isAfter(limite)) {
            falhas.removeFirst();
        }
    }
}
