package br.com.sicomat.config;

import br.com.sicomat.usuario.application.AutenticacaoContaService;
import br.com.sicomat.usuario.application.AutenticacaoContaService.Status;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class AutenticacaoProvider implements AuthenticationProvider {

    private final AutenticacaoContaService autenticacao;

    public AutenticacaoProvider(AutenticacaoContaService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String matricula = authentication.getName() == null ? "" : authentication.getName().trim();
        String senha = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();

        AutenticacaoContaService.Validacao validacao = autenticacao.validar(matricula, senha);
        return switch (validacao.status()) {
            case AUTENTICADA -> new UsernamePasswordAuthenticationToken(
                    validacao.usuario(), null, validacao.usuario().getAuthorities());
            case BLOQUEADA -> throw new LockedException("Credencial temporariamente bloqueada.");
            case INATIVA -> throw new DisabledException("A conta está inativa.");
            case INVALIDA -> throw new BadCredentialsException("Matrícula ou senha inválida.");
        };
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}