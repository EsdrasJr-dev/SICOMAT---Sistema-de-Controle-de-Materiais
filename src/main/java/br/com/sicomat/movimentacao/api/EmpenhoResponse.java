package br.com.sicomat.movimentacao.api;

import br.com.sicomat.movimentacao.StatusEmpenho;
import java.time.Instant;
import java.util.List;

public record EmpenhoResponse(
        Long id,
        StatusEmpenho status,
        Instant dataHora,
        String observacao,
        Long equipeId,
        String equipeNome,
        Long recebedorId,
        String recebedorNome,
        String operadorNome,
        Instant confirmadoEm,
        String metodoConfirmacao,
        String justificativa,
        List<ItemEmpenhoResponse> itens) {
}
