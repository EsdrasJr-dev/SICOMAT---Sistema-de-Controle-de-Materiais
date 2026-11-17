package br.com.sicomat.equipe.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarEquipeRequest(@NotBlank @Size(max = 120) String nome) {
}
