package br.com.sicomat.estoque.infrastructure.persistence;

import br.com.sicomat.estoque.StatusItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemMaterialJpaEntityTest {

    @Test
    void copiaSetorDoMaterialParaOItemPersistido() {
        MaterialJpaEntity material = new MaterialJpaEntity("Almoxarifado", "MAT-01", "Material de teste");

        ItemMaterialJpaEntity item = new ItemMaterialJpaEntity(
                material, "PAT-01", StatusItem.DISPONIVEL);

        assertEquals("Almoxarifado", item.getSetor());
    }
}