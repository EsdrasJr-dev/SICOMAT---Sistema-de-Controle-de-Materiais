package br.com.sicomat.movimentacao.api;

import br.com.sicomat.movimentacao.CondicaoItem;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ItemDevolucaoRequest(
        @NotNull @Positive Long itemEmpenhoId,
        @NotNull CondicaoItem condicao,
        @Size(max = 500) String observacao) {
}
