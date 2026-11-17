package br.com.sicomat.equipe.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarRecebedorRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 40) String matricula,
        @NotBlank @Pattern(regexp = "\\d{6}") String pinInicial) {
}
