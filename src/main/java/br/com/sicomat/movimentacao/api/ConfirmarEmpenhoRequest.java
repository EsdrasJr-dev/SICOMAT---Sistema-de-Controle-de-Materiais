package br.com.sicomat.movimentacao.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfirmarEmpenhoRequest(
        @NotBlank @Pattern(regexp = "\\d{6}") String pin,
        @NotBlank @Size(max = 500) String justificativa) {
}
