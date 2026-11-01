package br.com.sicomat.estoque.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarMaterialRequest(
        @NotBlank @Size(max = 40) String codigo,
        @NotBlank @Size(max = 120) String nome) {
}