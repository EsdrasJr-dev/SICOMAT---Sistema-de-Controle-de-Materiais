package br.com.sicomat.usuario;

import br.com.sicomat.seguranca.CodificadorSenha;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdministradorSistemaTest {

    @Test
    void contaUnicaDoAdministradorDoSistemaNaoPodeSerInativada() {
        String hashSenha = CodificadorSenha.codificar("senha-de-teste-123");
        AdministradorSistema administrador = new AdministradorSistema(
                "Admin", "admin", hashSenha, "Geral");

        assertThrows(IllegalStateException.class,
                () -> administrador.inativarUsuario(administrador));

        assertTrue(administrador.isAtivo());
    }

    @Test
    void administradorDoSistemaPodeInativarContasDeEstoque() {
        String hashSenha = CodificadorSenha.codificar("senha-de-teste-123");
        AdministradorEstoque administradorEstoque = new AdministradorEstoque(
                "Admin estoque", "admin-est", hashSenha, "Geral");
        AdministradorSistema administradorSistema = new AdministradorSistema(
                "Admin sistema", "admin-sis", hashSenha, "Geral", List.of(administradorEstoque));

        administradorSistema.inativarUsuario(administradorEstoque);

        assertFalse(administradorEstoque.isAtivo());
    }
}