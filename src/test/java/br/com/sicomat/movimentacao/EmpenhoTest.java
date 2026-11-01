package br.com.sicomat.movimentacao;

import br.com.sicomat.estoque.Estoque;
import br.com.sicomat.estoque.ItemMaterial;
import br.com.sicomat.estoque.Material;
import br.com.sicomat.estoque.StatusItem;
import br.com.sicomat.equipe.Equipe;
import br.com.sicomat.equipe.Recebedor;
import br.com.sicomat.seguranca.CodificadorSenha;
import br.com.sicomat.usuario.OperadorEstoque;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmpenhoTest {

    @Test
    void permitePinComoExcecaoEDevolucaoParcialComRegistroDaCondicao() {
        Estoque estoque = new Estoque("Geral");
        Material material = new Material("MAT-01", "Material de teste");
        estoque.adicionarMaterial(material);
        estoque.adicionarItem(material, "PAT-01");
        estoque.adicionarItem(material, "PAT-02");

        OperadorEstoque operador = new OperadorEstoque(
                "Operador", "op-01", CodificadorSenha.codificar("senha-operador-123"),
                "Noite", estoque, null);
        Recebedor recebedor = new Recebedor(
                "Recebedor", "rec-01", CodificadorSenha.codificar("1234"));
        recebedor.registrarTemplateFacial(new byte[] {1}, LocalDateTime.now());
        Equipe equipe = new Equipe("Equipe A", "Geral");

        List<ItemMaterial> itens = estoque.listarItensDisponiveis();
        Empenho empenho = operador.iniciarEmpenho(recebedor, equipe, itens);

        assertTrue(operador.confirmarComPin(empenho, "1234", "Leitor facial indisponível"));
        assertEquals(StatusEmpenho.ABERTO, empenho.getStatus());
        assertEquals(StatusItem.EMPENHADO, itens.get(0).getStatus());

        ItemEmpenho primeiro = empenho.getItens().get(0);
        Devolucao parcial = operador.registrarDevolucao(
                recebedor,
                List.of(new ItemDevolucao(primeiro, CondicaoItem.BOM, "")),
                "Primeira devolução");

        assertTrue(parcial.ehParcial());
        assertEquals(StatusEmpenho.DEVOLVIDO_PARCIALMENTE, empenho.getStatus());
        assertEquals(StatusItem.DISPONIVEL, primeiro.getItemMaterial().getStatus());

        ItemEmpenho segundo = empenho.getItens().get(1);
        operador.registrarDevolucao(
                recebedor,
                List.of(new ItemDevolucao(segundo, CondicaoItem.AVARIADO, "Avaria observada")),
                "Devolução final");

        assertEquals(StatusEmpenho.ENCERRADO, empenho.getStatus());
        assertEquals(StatusItem.AVARIADO, segundo.getItemMaterial().getStatus());
        assertEquals(1, estoque.listarItensDisponiveis().size());
    }
}