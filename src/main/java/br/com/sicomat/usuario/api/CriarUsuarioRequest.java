package br.com.sicomat.usuario.api;

import br.com.sicomat.usuario.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 40) String matricula,
        @NotBlank @Size(min = 8, max = 128) String senhaInicial,
        @NotNull PerfilUsuario perfil,
        @NotBlank @Size(max = 80) String setor,
        @Size(max = 40) String turno) {
}