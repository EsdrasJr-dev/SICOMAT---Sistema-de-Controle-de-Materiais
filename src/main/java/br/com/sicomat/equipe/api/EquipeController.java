package br.com.sicomat.equipe.api;

import br.com.sicomat.config.UsuarioAutenticado;
import br.com.sicomat.equipe.application.EquipeApplicationService;
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
@RequestMapping("/api/v1/equipes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_ESTOQUE', 'OPERADOR_ESTOQUE')")
public class EquipeController {

    private final EquipeApplicationService equipes;

    public EquipeController(EquipeApplicationService equipes) {
        this.equipes = equipes;
    }

    @GetMapping
    public List<EquipeResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return equipes.listar(usuario);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR_ESTOQUE')")
    public ResponseEntity<EquipeResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CriarEquipeRequest requisicao) {
        EquipeResponse equipe = equipes.cadastrar(usuario, requisicao);
        return ResponseEntity.created(URI.create("/api/v1/equipes/" + equipe.id())).body(equipe);
    }

    @PatchMapping("/{id}/inativacao")
    @PreAuthorize("hasRole('ADMINISTRADOR_ESTOQUE')")
    public ResponseEntity<Void> inativar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable long id) {
        equipes.inativar(usuario, id);
        return ResponseEntity.noContent().build();
    }
}
