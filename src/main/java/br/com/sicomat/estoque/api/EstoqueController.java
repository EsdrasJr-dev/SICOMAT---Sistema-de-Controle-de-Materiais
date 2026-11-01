package br.com.sicomat.estoque.api;

import br.com.sicomat.config.UsuarioAutenticado;
import br.com.sicomat.estoque.application.EstoqueApplicationService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EstoqueController {

    private final EstoqueApplicationService estoque;

    public EstoqueController(EstoqueApplicationService estoque) {
        this.estoque = estoque;
    }

    @GetMapping("/materiais")
    public List<MaterialResponse> listarMateriais(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return estoque.listarMateriais(usuario);
    }

    @PostMapping("/materiais")
    @PreAuthorize("hasRole('ADMINISTRADOR_ESTOQUE')")
    public ResponseEntity<MaterialResponse> cadastrarMaterial(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CriarMaterialRequest requisicao) {
        MaterialResponse material = estoque.cadastrarMaterial(usuario, requisicao);
        return ResponseEntity.created(URI.create("/api/v1/materiais/" + material.id())).body(material);
    }

    @PutMapping("/materiais/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR_ESTOQUE')")
    public MaterialResponse editarMaterial(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable long id,
            @Valid @RequestBody CriarMaterialRequest requisicao) {
        return estoque.editarMaterial(usuario, id, requisicao);
    }

    @PatchMapping("/materiais/{id}/inativacao")
    @PreAuthorize("hasRole('ADMINISTRADOR_ESTOQUE')")
    public ResponseEntity<Void> inativarMaterial(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable long id) {
        estoque.inativarMaterial(usuario, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/materiais/{id}/itens")
    @PreAuthorize("hasRole('ADMINISTRADOR_ESTOQUE')")
    public ResponseEntity<ItemMaterialResponse> cadastrarItem(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable long id,
            @Valid @RequestBody CadastrarItemRequest requisicao) {
        ItemMaterialResponse item = estoque.cadastrarItem(usuario, id, requisicao);
        return ResponseEntity.created(URI.create("/api/v1/materiais/" + id + "/itens/" + item.id())).body(item);
    }

    @GetMapping("/itens/disponiveis")
    public List<ItemMaterialResponse> listarItensDisponiveis(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return estoque.listarItensDisponiveis(usuario);
    }
}