package br.com.sicomat.equipe.application;

import br.com.sicomat.config.UsuarioAutenticado;
import br.com.sicomat.equipe.api.CriarEquipeRequest;
import br.com.sicomat.equipe.api.EquipeResponse;
import br.com.sicomat.equipe.infrastructure.persistence.EquipeJpaEntity;
import br.com.sicomat.equipe.infrastructure.persistence.EquipeJpaRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EquipeApplicationService {

    private final EquipeJpaRepository equipes;

    public EquipeApplicationService(EquipeJpaRepository equipes) {
        this.equipes = equipes;
    }

    @Transactional(readOnly = true)
    public List<EquipeResponse> listar(UsuarioAutenticado usuario) {
        return equipes.findAllBySetorAndAtivoTrueOrderByNomeAsc(usuario.setor()).stream()
                .map(EquipeApplicationService::resposta)
                .toList();
    }

    @Transactional
    public EquipeResponse cadastrar(UsuarioAutenticado usuario, CriarEquipeRequest requisicao) {
        String nome = requisicao.nome().trim();
        if (equipes.existsBySetorAndNomeIgnoreCase(usuario.setor(), nome)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma equipe com esse nome neste setor.");
        }
        return resposta(equipes.save(new EquipeJpaEntity(usuario.setor(), nome)));
    }

    @Transactional
    public void inativar(UsuarioAutenticado usuario, long id) {
        EquipeJpaEntity equipe = equipes.findByIdAndSetor(id, usuario.setor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Equipe não encontrada neste setor."));
        equipe.inativar();
    }

    private static EquipeResponse resposta(EquipeJpaEntity equipe) {
        return new EquipeResponse(equipe.getId(), equipe.getNome(), equipe.isAtivo());
    }
}
