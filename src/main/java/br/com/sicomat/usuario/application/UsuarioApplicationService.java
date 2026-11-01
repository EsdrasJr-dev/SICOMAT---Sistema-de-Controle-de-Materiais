package br.com.sicomat.usuario.application;

import br.com.sicomat.seguranca.CodificadorSenha;
import br.com.sicomat.usuario.PerfilUsuario;
import br.com.sicomat.usuario.api.CriarUsuarioRequest;
import br.com.sicomat.usuario.api.UsuarioResponse;
import br.com.sicomat.usuario.infrastructure.persistence.UsuarioJpaEntity;
import br.com.sicomat.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioApplicationService {

    private final UsuarioJpaRepository usuarios;

    public UsuarioApplicationService(UsuarioJpaRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findAllByOrderByNomeAsc().stream().map(UsuarioApplicationService::resposta).toList();
    }

    @Transactional
    public UsuarioResponse cadastrar(CriarUsuarioRequest requisicao) {
        if (requisicao.perfil() == PerfilUsuario.ADMINISTRADOR_SISTEMA) {
            throw new IllegalArgumentException("A conta única do administrador do sistema só pode ser criada na carga inicial.");
        }
        if (requisicao.perfil() == PerfilUsuario.OPERADOR_ESTOQUE
                && (requisicao.turno() == null || requisicao.turno().isBlank())) {
            throw new IllegalArgumentException("Informe o turno do operador do estoque.");
        }
        if (usuarios.existsByMatriculaIgnoreCase(requisicao.matricula().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe usuário com essa matrícula.");
        }

        UsuarioJpaEntity usuario = new UsuarioJpaEntity(
                requisicao.nome().trim(),
                requisicao.matricula().trim(),
                CodificadorSenha.codificar(requisicao.senhaInicial()),
                requisicao.perfil(),
                requisicao.setor().trim(),
                requisicao.turno() == null ? null : requisicao.turno().trim());
        return resposta(usuarios.save(usuario));
    }

    @Transactional
    public void inativar(long id) {
        UsuarioJpaEntity usuario = usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        usuario.inativar();
        usuarios.save(usuario);
    }

    private static UsuarioResponse resposta(UsuarioJpaEntity usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getMatricula(),
                usuario.getPerfil(),
                usuario.getSetor(),
                usuario.getTurno(),
                usuario.isAtivo());
    }
}