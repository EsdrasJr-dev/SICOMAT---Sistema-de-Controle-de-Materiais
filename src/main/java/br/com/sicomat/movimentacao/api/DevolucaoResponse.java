package br.com.sicomat.movimentacao.api;

import java.time.Instant;
import java.util.List;

public record DevolucaoResponse(
        Long id,
        Long empenhoId,
        Instant dataHora,
        String observacao,
        String registradoPorNome,
        String devolvidoPorNome,
        List<ItemDevolucaoResponse> itens) {
}
