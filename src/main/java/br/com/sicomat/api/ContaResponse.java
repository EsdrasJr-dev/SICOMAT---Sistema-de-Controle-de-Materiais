package br.com.sicomat.api;

import br.com.sicomat.usuario.PerfilUsuario;

public record ContaResponse(Long id, String nome, String matricula, PerfilUsuario perfil, String setor, String turno) {
}
