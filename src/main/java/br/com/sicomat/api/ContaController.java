package br.com.sicomat.api;

import br.com.sicomat.config.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conta")
public class ContaController {

    @GetMapping
    public ContaResponse consultar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return new ContaResponse(
                usuario.id(), usuario.nome(), usuario.matricula(), usuario.perfil(), usuario.setor(), usuario.turno());
    }
}
