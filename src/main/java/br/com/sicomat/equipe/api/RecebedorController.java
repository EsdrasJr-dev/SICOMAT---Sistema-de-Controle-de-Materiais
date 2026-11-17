package br.com.sicomat.equipe.api;

import br.com.sicomat.config.UsuarioAutenticado;
import br.com.sicomat.equipe.application.RecebedorApplicationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recebedores")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_ESTOQUE', 'OPERADOR_ESTOQUE')")
public class RecebedorController {

    private final RecebedorApplicationService recebedores;

    public RecebedorController(RecebedorApplicationService recebedores) {
        this.recebedores = recebedores;
    }

    @GetMapping
    public List<RecebedorResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return recebedores.listar(usuario);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR_ESTOQUE')")
    public ResponseEntity<RecebedorResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CriarRecebedorRequest requisicao) {
        RecebedorResponse recebedor = recebedores.cadastrar(usuario, requisicao);
        return ResponseEntity.created(URI.create("/api/v1/recebedores/" + recebedor.id())).body(recebedor);
    }

    @PatchMapping("/{id}/inativacao")
    @PreAuthorize("hasRole('ADMINISTRADOR_ESTOQUE')")
    public ResponseEntity<Void> inativar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable long id) {
        recebedores.inativar(usuario, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/pin")
    @PreAuthorize("hasRole('ADMINISTRADOR_ESTOQUE')")
    public ResponseEntity<Void> redefinirPin(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable long id,
            @Valid @RequestBody RedefinirPinRequest requisicao) {
        recebedores.redefinirPin(usuario, id, requisicao.novoPin());
        return ResponseEntity.noContent().build();
    }
}
