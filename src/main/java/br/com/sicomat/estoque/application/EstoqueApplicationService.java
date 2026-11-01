package br.com.sicomat.estoque.application;

import br.com.sicomat.config.UsuarioAutenticado;
import br.com.sicomat.estoque.StatusItem;
import br.com.sicomat.estoque.api.CadastrarItemRequest;
import br.com.sicomat.estoque.api.CriarMaterialRequest;
import br.com.sicomat.estoque.api.ItemMaterialResponse;
import br.com.sicomat.estoque.api.MaterialResponse;
import br.com.sicomat.estoque.infrastructure.persistence.ItemMaterialJpaEntity;
import br.com.sicomat.estoque.infrastructure.persistence.ItemMaterialJpaRepository;
import br.com.sicomat.estoque.infrastructure.persistence.MaterialJpaEntity;
import br.com.sicomat.estoque.infrastructure.persistence.MaterialJpaRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EstoqueApplicationService {

    private final MaterialJpaRepository materiais;
    private final ItemMaterialJpaRepository itens;

    public EstoqueApplicationService(MaterialJpaRepository materiais, ItemMaterialJpaRepository itens) {
        this.materiais = materiais;
        this.itens = itens;
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> listarMateriais(UsuarioAutenticado usuario) {
        return materiais.findAllBySetorOrderByNomeAsc(usuario.setor()).stream()
                .map(this::respostaMaterial)
                .toList();
    }

    @Transactional
    public MaterialResponse cadastrarMaterial(UsuarioAutenticado usuario, CriarMaterialRequest requisicao) {
        String setor = usuario.setor();
        String codigo = requisicao.codigo().trim();
        if (materiais.existsBySetorAndCodigoIgnoreCase(setor, codigo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe material com esse código neste estoque.");
        }
        MaterialJpaEntity material = materiais.save(
                new MaterialJpaEntity(setor, codigo, requisicao.nome().trim()));
        return respostaMaterial(material);
    }

    @Transactional
    public MaterialResponse editarMaterial(UsuarioAutenticado usuario, long id, CriarMaterialRequest requisicao) {
        MaterialJpaEntity material = buscarDoSetor(usuario, id);
        String codigo = requisicao.codigo().trim();
        boolean codigoEmUso = materiais.existsBySetorAndCodigoIgnoreCase(usuario.setor(), codigo)
                && !material.getCodigo().equalsIgnoreCase(codigo);
        if (codigoEmUso) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe material com esse código neste estoque.");
        }
        material.editar(codigo, requisicao.nome().trim());
        return respostaMaterial(material);
    }

    @Transactional
    public void inativarMaterial(UsuarioAutenticado usuario, long id) {
        MaterialJpaEntity material = buscarDoSetor(usuario, id);
        material.inativar();
        for (ItemMaterialJpaEntity item : itens.findAllByMaterial_Id(id)) {
            if (item.getStatus() == StatusItem.DISPONIVEL || item.getStatus() == StatusItem.AVARIADO) {
                item.setStatus(StatusItem.INATIVO);
            }
        }
    }

    @Transactional
    public ItemMaterialResponse cadastrarItem(UsuarioAutenticado usuario, long materialId, CadastrarItemRequest requisicao) {
        MaterialJpaEntity material = buscarDoSetor(usuario, materialId);
        if (!material.isAtivo()) throw new IllegalStateException("Não é possível incluir itens em um material inativo.");

        String patrimonio = requisicao.numeroPatrimonio().trim();
        if (itens.existsBySetorAndNumeroPatrimonioIgnoreCase(usuario.setor(), patrimonio)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe item com esse patrimônio.");
        }

        ItemMaterialJpaEntity item = itens.save(
                new ItemMaterialJpaEntity(material, patrimonio, StatusItem.DISPONIVEL));
        return respostaItem(item);
    }

    @Transactional(readOnly = true)
    public List<ItemMaterialResponse> listarItensDisponiveis(UsuarioAutenticado usuario) {
        return itens.findAllByMaterial_SetorAndStatusOrderByNumeroPatrimonio(usuario.setor(), StatusItem.DISPONIVEL)
                .stream()
                .filter(item -> item.getMaterial().isAtivo())
                .map(EstoqueApplicationService::respostaItem)
                .toList();
    }

    private MaterialJpaEntity buscarDoSetor(UsuarioAutenticado usuario, long id) {
        return materiais.findByIdAndSetor(id, usuario.setor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado neste estoque."));
    }

    private MaterialResponse respostaMaterial(MaterialJpaEntity material) {
        return new MaterialResponse(
                material.getId(),
                material.getCodigo(),
                material.getNome(),
                material.isAtivo(),
                itens.countByMaterial_Id(material.getId()),
                itens.countByMaterial_IdAndStatus(material.getId(), StatusItem.DISPONIVEL));
    }

    private static ItemMaterialResponse respostaItem(ItemMaterialJpaEntity item) {
        return new ItemMaterialResponse(
                item.getId(),
                item.getNumeroPatrimonio(),
                item.getMaterial().getCodigo(),
                item.getMaterial().getNome(),
                item.getStatus());
    }
}