package br.com.sicomat.equipe.application;

import br.com.sicomat.config.UsuarioAutenticado;
import br.com.sicomat.equipe.api.CriarRecebedorRequest;
import br.com.sicomat.equipe.api.RecebedorResponse;
import br.com.sicomat.equipe.infrastructure.persistence.RecebedorJpaEntity;
import br.com.sicomat.equipe.infrastructure.persistence.RecebedorJpaRepository;
import br.com.sicomat.seguranca.CodificadorSenha;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecebedorApplicationService {

    private final RecebedorJpaRepository recebedores;

    public RecebedorApplicationService(RecebedorJpaRepository recebedores) {
        this.recebedores = recebedores;
    }

    @Transactional(readOnly = true)
    public List<RecebedorResponse> listar(UsuarioAutenticado usuario) {
        return recebedores.findAllBySetorAndAtivoTrueOrderByNomeAsc(usuario.setor()).stream()
                .map(RecebedorApplicationService::resposta)
                .toList();
    }

    @Transactional
    public RecebedorResponse cadastrar(UsuarioAutenticado usuario, CriarRecebedorRequest requisicao) {
        String matricula = requisicao.matricula().trim();
        if (recebedores.existsBySetorAndMatriculaIgnoreCase(usuario.setor(), matricula)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe recebedor com essa matrícula neste setor.");
        }
        RecebedorJpaEntity recebedor = new RecebedorJpaEntity(
                usuario.setor(), requisicao.nome().trim(), matricula, CodificadorSenha.codificar(requisicao.pinInicial()));
        return resposta(recebedores.save(recebedor));
    }

    @Transactional
    public void inativar(UsuarioAutenticado usuario, long id) {
        RecebedorJpaEntity recebedor = buscar(usuario, id);
        recebedor.inativar();
    }

    @Transactional
    public void redefinirPin(UsuarioAutenticado usuario, long id, String novoPin) {
        RecebedorJpaEntity recebedor = buscar(usuario, id);
        recebedor.redefinirPin(CodificadorSenha.codificar(novoPin));
    }

    private RecebedorJpaEntity buscar(UsuarioAutenticado usuario, long id) {
        return recebedores.findByIdAndSetor(id, usuario.setor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recebedor não encontrado neste setor."));
    }

    private static RecebedorResponse resposta(RecebedorJpaEntity recebedor) {
        return new RecebedorResponse(recebedor.getId(), recebedor.getNome(), recebedor.getMatricula(), recebedor.isAtivo());
    }
}
