package br.com.sicomat.movimentacao.api;

import br.com.sicomat.movimentacao.CondicaoItem;

public record ItemDevolucaoResponse(
        Long itemEmpenhoId,
        String numeroPatrimonio,
        CondicaoItem condicao,
        String observacao) {
}
