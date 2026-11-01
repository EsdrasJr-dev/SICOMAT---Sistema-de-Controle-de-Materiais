package br.com.sicomat.estoque.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastrarItemRequest(@NotBlank @Size(max = 80) String numeroPatrimonio) {
}