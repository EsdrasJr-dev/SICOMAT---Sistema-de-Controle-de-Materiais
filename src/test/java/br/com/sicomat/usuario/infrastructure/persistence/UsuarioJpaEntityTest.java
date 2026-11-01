package br.com.sicomat.usuario.infrastructure.persistence;

import br.com.sicomat.usuario.PerfilUsuario;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioJpaEntityTest {

    @Test
    void bloqueioDaContaExpiraAposCincoMinutosEJanelaReinicia() {
        UsuarioJpaEntity usuario = new UsuarioJpaEntity(
                "Operador", "op-01", "hash-teste", PerfilUsuario.OPERADOR_ESTOQUE, "Geral", "Noite");
        LocalDateTime inicio = LocalDateTime.of(2026, 1, 1, 12, 0);

        for (int falha = 0; falha < 5; falha++) {
            usuario.registrarFalha(inicio.plusSeconds(falha));
        }
        LocalDateTime bloqueioIniciado = inicio.plusSeconds(4);

        assertTrue(usuario.estaBloqueado(bloqueioIniciado.plusMinutes(4)));
        assertFalse(usuario.estaBloqueado(bloqueioIniciado.plusMinutes(5)));

        usuario.registrarFalha(bloqueioIniciado.plusMinutes(5));

        assertEquals(1, usuario.getFalhasLogin());
        assertFalse(usuario.estaBloqueado(bloqueioIniciado.plusMinutes(5)));
    }
}