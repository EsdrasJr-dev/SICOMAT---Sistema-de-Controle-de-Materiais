package br.com.sicomat.config;

import br.com.sicomat.usuario.PerfilUsuario;
import br.com.sicomat.usuario.infrastructure.persistence.UsuarioJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UsuarioAutenticado(
        Long id,
        String nome,
        String matricula,
        PerfilUsuario perfil,
        String setor,
        String turno) implements UserDetails {

    public static UsuarioAutenticado de(UsuarioJpaEntity usuario) {
        return new UsuarioAutenticado(
                usuario.getId(),
                usuario.getNome(),
                usuario.getMatricula(),
                usuario.getPerfil(),
                usuario.getSetor(),
                usuario.getTurno());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override public String getPassword() { return ""; }
    @Override public String getUsername() { return matricula; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}