package br.com.sicomat.usuario.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank @Size(min = 15, max = 128) String novaSenha) {
}
