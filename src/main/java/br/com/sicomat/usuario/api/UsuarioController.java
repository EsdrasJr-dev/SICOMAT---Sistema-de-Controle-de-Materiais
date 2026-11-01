package br.com.sicomat.usuario.api;

import br.com.sicomat.usuario.application.UsuarioApplicationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR_SISTEMA')")
public class UsuarioController {

    private final UsuarioApplicationService usuarios;

    public UsuarioController(UsuarioApplicationService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarios.listar();
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CriarUsuarioRequest requisicao) {
        UsuarioResponse usuario = usuarios.cadastrar(requisicao);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + usuario.id())).body(usuario);
    }

    @PatchMapping("/{id}/inativacao")
    public ResponseEntity<Void> inativar(@PathVariable long id) {
        usuarios.inativar(id);
        return ResponseEntity.noContent().build();
    }
}