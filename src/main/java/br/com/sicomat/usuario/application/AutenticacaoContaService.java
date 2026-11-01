package br.com.sicomat.usuario.application;

import br.com.sicomat.config.UsuarioAutenticado;
import br.com.sicomat.seguranca.CodificadorSenha;
import br.com.sicomat.usuario.infrastructure.persistence.UsuarioJpaEntity;
import br.com.sicomat.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoContaService {

    private static final String HASH_DUMMY = CodificadorSenha.codificar("senha-dummy-nao-utilizada");

    private final UsuarioJpaRepository usuarios;

    public AutenticacaoContaService(UsuarioJpaRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional
    public Validacao validar(String matricula, String senha) {
        UsuarioJpaEntity usuario = usuarios.buscarParaAutenticacao(matricula).orElse(null);
        if (usuario == null) {
            CodificadorSenha.verificar(senha, HASH_DUMMY);
            return new Validacao(Status.INVALIDA, null);
        }

        if (!usuario.isAtivo()) return new Validacao(Status.INATIVA, null);

        LocalDateTime agora = LocalDateTime.now();
        if (usuario.estaBloqueado(agora)) return new Validacao(Status.BLOQUEADA, null);

        if (!CodificadorSenha.verificar(senha, usuario.getSenhaHash())) {
            usuario.registrarFalha(agora);
            usuarios.save(usuario);
            return new Validacao(usuario.estaBloqueado(agora) ? Status.BLOQUEADA : Status.INVALIDA, null);
        }

        usuario.limparFalhas();
        usuarios.save(usuario);
        return new Validacao(Status.AUTENTICADA, UsuarioAutenticado.de(usuario));
    }

    public enum Status { AUTENTICADA, INVALIDA, INATIVA, BLOQUEADA }

    public record Validacao(Status status, UsuarioAutenticado usuario) {
    }
}