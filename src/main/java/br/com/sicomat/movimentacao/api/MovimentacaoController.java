package br.com.sicomat.movimentacao.api;

import br.com.sicomat.api.PaginaResponse;
import br.com.sicomat.config.UsuarioAutenticado;
import br.com.sicomat.movimentacao.application.MovimentacaoApplicationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/empenhos")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_ESTOQUE', 'OPERADOR_ESTOQUE')")
public class MovimentacaoController {

    private final MovimentacaoApplicationService movimentacoes;

    public MovimentacaoController(MovimentacaoApplicationService movimentacoes) {
        this.movimentacoes = movimentacoes;
    }

    @GetMapping("/pendentes")
    public PaginaResponse<EmpenhoResponse> listarPendentes(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return pagina(movimentacoes.listarPendentes(usuario, pagina, tamanho));
    }

    @GetMapping("/abertos")
    public PaginaResponse<EmpenhoResponse> listarAbertos(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return pagina(movimentacoes.listarAbertos(usuario, pagina, tamanho));
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERADOR_ESTOQUE')")
    public ResponseEntity<EmpenhoResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CriarEmpenhoRequest requisicao) {
        EmpenhoResponse empenho = movimentacoes.criar(usuario, requisicao);
        return ResponseEntity.created(URI.create("/api/v1/empenhos/" + empenho.id())).body(empenho);
    }

    @GetMapping("/{id}")
    public EmpenhoResponse buscar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable long id) {
        return movimentacoes.buscar(usuario, id);
    }

    @PostMapping("/{id}/confirmacao")
    @PreAuthorize("hasRole('OPERADOR_ESTOQUE')")
    public ConfirmacaoEmpenhoResponse confirmar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable long id,
            @Valid @RequestBody ConfirmarEmpenhoRequest requisicao) {
        return movimentacoes.confirmar(usuario, id, requisicao);
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasRole('OPERADOR_ESTOQUE')")
    public ResponseEntity<Void> cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable long id) {
        movimentacoes.cancelar(usuario, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/devolucoes")
    @PreAuthorize("hasRole('OPERADOR_ESTOQUE')")
    public ResponseEntity<DevolucaoResponse> registrarDevolucao(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable long id,
            @Valid @RequestBody RegistrarDevolucaoRequest requisicao) {
        DevolucaoResponse devolucao = movimentacoes.registrarDevolucao(usuario, id, requisicao);
        return ResponseEntity.created(URI.create("/api/v1/empenhos/" + id + "/devolucoes/" + devolucao.id()))
                .body(devolucao);
    }

    @GetMapping("/{id}/devolucoes")
    public List<DevolucaoResponse> listarDevolucoes(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable long id) {
        return movimentacoes.listarDevolucoes(usuario, id);
    }

    @PostMapping("/{id}/itens/{itemEmpenhoId}/extravio")
    @PreAuthorize("hasRole('ADMINISTRADOR_ESTOQUE')")
    public ResponseEntity<Void> registrarExtravio(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable long id,
            @PathVariable long itemEmpenhoId) {
        movimentacoes.registrarExtravio(usuario, id, itemEmpenhoId);
        return ResponseEntity.noContent().build();
    }

    private static PaginaResponse<EmpenhoResponse> pagina(Page<EmpenhoResponse> page) {
        return PaginaResponse.de(page);
    }
}
