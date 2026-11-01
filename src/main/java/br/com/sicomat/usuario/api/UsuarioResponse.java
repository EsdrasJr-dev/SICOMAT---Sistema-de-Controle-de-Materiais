package br.com.sicomat.usuario.api;

import br.com.sicomat.usuario.PerfilUsuario;

public record UsuarioResponse(
        Long id,
        String nome,
        String matricula,
        PerfilUsuario perfil,
        String setor,
        String turno,
        boolean ativo) {
}