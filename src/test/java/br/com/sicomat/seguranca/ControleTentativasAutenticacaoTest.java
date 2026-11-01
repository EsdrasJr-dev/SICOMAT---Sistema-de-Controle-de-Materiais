package br.com.sicomat.seguranca;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControleTentativasAutenticacaoTest {

    @Test
    void bloqueiaNaQuintaFalhaEPermiteRedefinirOEstado() {
        ControleTentativasAutenticacao controle = new ControleTentativasAutenticacao();

        for (int falha = 1; falha < 5; falha++) {
            assertFalse(controle.registrarFalha());
        }
        assertTrue(controle.registrarFalha());
        assertThrows(IllegalStateException.class, controle::exigirTentativaPermitida);

        controle.redefinir();

        assertDoesNotThrow(controle::exigirTentativaPermitida);
    }
}