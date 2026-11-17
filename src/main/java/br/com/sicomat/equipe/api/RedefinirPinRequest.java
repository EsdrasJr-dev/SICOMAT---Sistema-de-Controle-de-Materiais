package br.com.sicomat.equipe.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RedefinirPinRequest(@NotBlank @Pattern(regexp = "\\d{6}") String novoPin) {
}
