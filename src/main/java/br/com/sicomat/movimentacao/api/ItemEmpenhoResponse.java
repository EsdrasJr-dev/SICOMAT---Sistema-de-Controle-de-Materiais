package br.com.sicomat.movimentacao.api;

import br.com.sicomat.movimentacao.SituacaoItemEmpenho;
import br.com.sicomat.estoque.StatusItem;
import java.time.Instant;

public record ItemEmpenhoResponse(
        Long id,
        Long itemMaterialId,
        String numeroPatrimonio,
        String codigoMaterial,
        String nomeMaterial,
        StatusItem statusItem,
        SituacaoItemEmpenho situacao,
        Instant extraviadoEm) {
}
