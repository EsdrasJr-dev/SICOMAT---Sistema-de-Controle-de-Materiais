package br.com.sicomat.movimentacao.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RegistrarDevolucaoRequest(
        @NotNull @Positive Long devolvidoPorId,
        @NotEmpty @Size(max = 50) List<@Valid ItemDevolucaoRequest> itens,
        @Size(max = 500) String observacao) {
}
