package br.com.sicomat.estoque.api;

import br.com.sicomat.estoque.StatusItem;

public record ItemMaterialResponse(
        Long id,
        String numeroPatrimonio,
        String codigoMaterial,
        String nomeMaterial,
        StatusItem status) {
}