package br.com.sicomat.movimentacao.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CriarEmpenhoRequest(
        @NotNull @Positive Long recebedorId,
        @NotNull @Positive Long equipeId,
        @NotEmpty @Size(max = 50) List<@NotNull @Positive Long> itemMaterialIds,
        @Size(max = 500) String observacao) {
}
