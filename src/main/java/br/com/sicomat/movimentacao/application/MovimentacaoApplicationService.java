package br.com.sicomat.movimentacao.application;

import br.com.sicomat.config.UsuarioAutenticado;
import br.com.sicomat.estoque.StatusItem;
import br.com.sicomat.estoque.api.ItemMaterialResponse;
import br.com.sicomat.estoque.infrastructure.persistence.ItemMaterialJpaEntity;
import br.com.sicomat.estoque.infrastructure.persistence.ItemMaterialJpaRepository;
import br.com.sicomat.equipe.infrastructure.persistence.EquipeJpaEntity;
import br.com.sicomat.equipe.infrastructure.persistence.EquipeJpaRepository;
import br.com.sicomat.equipe.infrastructure.persistence.RecebedorJpaEntity;
import br.com.sicomat.equipe.infrastructure.persistence.RecebedorJpaRepository;
import br.com.sicomat.movimentacao.CondicaoItem;
import br.com.sicomat.movimentacao.SituacaoItemEmpenho;
import br.com.sicomat.movimentacao.StatusEmpenho;
import br.com.sicomat.movimentacao.api.CriarEmpenhoRequest;
import br.com.sicomat.movimentacao.api.ConfirmarEmpenhoRequest;
import br.com.sicomat.movimentacao.api.ConfirmacaoEmpenhoResponse;
import br.com.sicomat.movimentacao.api.DevolucaoResponse;
import br.com.sicomat.movimentacao.api.EmpenhoResponse;
import br.com.sicomat.movimentacao.api.ItemDevolucaoRequest;
import br.com.sicomat.movimentacao.api.ItemDevolucaoResponse;
import br.com.sicomat.movimentacao.api.ItemEmpenhoResponse;
import br.com.sicomat.movimentacao.api.RegistrarDevolucaoRequest;
import br.com.sicomat.movimentacao.infrastructure.persistence.DevolucaoJpaEntity;
import br.com.sicomat.movimentacao.infrastructure.persistence.DevolucaoJpaRepository;
import br.com.sicomat.movimentacao.infrastructure.persistence.EmpenhoJpaEntity;
import br.com.sicomat.movimentacao.infrastructure.persistence.EmpenhoJpaRepository;
import br.com.sicomat.movimentacao.infrastructure.persistence.ItemDevolucaoJpaEntity;
import br.com.sicomat.movimentacao.infrastructure.persistence.ItemEmpenhoJpaEntity;
import br.com.sicomat.movimentacao.infrastructure.persistence.ItemEmpenhoJpaRepository;
import br.com.sicomat.usuario.PerfilUsuario;
import br.com.sicomat.usuario.infrastructure.persistence.UsuarioJpaEntity;
import br.com.sicomat.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MovimentacaoApplicationService {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final EmpenhoJpaRepository empenhos;
    private final ItemEmpenhoJpaRepository itensEmpenho;
    private final DevolucaoJpaRepository devolucoes;
    private final ItemMaterialJpaRepository itensMaterial;
    private final RecebedorJpaRepository recebedores;
    private final EquipeJpaRepository equipes;
    private final UsuarioJpaRepository usuarios;

    public MovimentacaoApplicationService(
            EmpenhoJpaRepository empenhos,
            ItemEmpenhoJpaRepository itensEmpenho,
            DevolucaoJpaRepository devolucoes,
            ItemMaterialJpaRepository itensMaterial,
            RecebedorJpaRepository recebedores,
            EquipeJpaRepository equipes,
            UsuarioJpaRepository usuarios) {
        this.empenhos = empenhos;
        this.itensEmpenho = itensEmpenho;
        this.devolucoes = devolucoes;
        this.itensMaterial = itensMaterial;
        this.recebedores = recebedores;
        this.equipes = equipes;
        this.usuarios = usuarios;
    }

    @Transactional
    public EmpenhoResponse criar(UsuarioAutenticado autenticado, CriarEmpenhoRequest requisicao) {
        List<Long> idsItens = requisicao.itemMaterialIds();
        Set<Long> idsUnicos = new HashSet<>(idsItens);
        if (idsUnicos.size() != idsItens.size()) {
            throw new IllegalArgumentException("O mesmo item físico não pode aparecer duas vezes no empenho.");
        }

        UsuarioJpaEntity operador = buscarOperador(autenticado);
        RecebedorJpaEntity recebedor = recebedores.findByIdAndSetor(requisicao.recebedorId(), autenticado.setor())
                .filter(RecebedorJpaEntity::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recebedor ativo não encontrado neste setor."));
        EquipeJpaEntity equipe = equipes.findByIdAndSetor(requisicao.equipeId(), autenticado.setor())
                .filter(EquipeJpaEntity::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipe ativa não encontrada neste setor."));

        List<ItemMaterialJpaEntity> itens = itensMaterial.buscarPorIdsDoSetorParaAtualizacao(
                new ArrayList<>(idsUnicos), autenticado.setor());
        if (itens.size() != idsUnicos.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Um ou mais itens não foram encontrados neste estoque.");
        }
        for (ItemMaterialJpaEntity item : itens) {
            if (item.getStatus() != StatusItem.DISPONIVEL || item.isReservado() || !item.getMaterial().isAtivo()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Um ou mais itens não estão disponíveis.");
            }
        }

        EmpenhoJpaEntity empenho = new EmpenhoJpaEntity(
                autenticado.setor(), operador, recebedor, equipe, requisicao.observacao());
        for (ItemMaterialJpaEntity item : itens) {
            empenho.adicionarItem(new ItemEmpenhoJpaEntity(empenho, item));
        }
        empenhos.saveAndFlush(empenho);
        for (ItemMaterialJpaEntity item : itens) item.reservarPara(empenho);
        itensMaterial.saveAll(itens);
        return resposta(empenho);
    }

    @Transactional(readOnly = true)
    public Page<EmpenhoResponse> listarPendentes(UsuarioAutenticado usuario, int pagina, int tamanho) {
        return listarPorStatus(usuario, pagina, tamanho, List.of(StatusEmpenho.PENDENTE));
    }

    @Transactional(readOnly = true)
    public Page<EmpenhoResponse> listarAbertos(UsuarioAutenticado usuario, int pagina, int tamanho) {
        return listarPorStatus(usuario, pagina, tamanho,
                List.of(StatusEmpenho.ABERTO, StatusEmpenho.DEVOLVIDO_PARCIALMENTE));
    }

    @Transactional(readOnly = true)
    public EmpenhoResponse buscar(UsuarioAutenticado usuario, long id) {
        EmpenhoJpaEntity empenho = empenhos.findByIdAndSetor(id, usuario.setor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empenho não encontrado neste setor."));
        return resposta(empenho);
    }

    @Transactional
    public ConfirmacaoEmpenhoResponse confirmar(
            UsuarioAutenticado autenticado, long id, ConfirmarEmpenhoRequest requisicao) {
        EmpenhoJpaEntity empenho = buscarParaAtualizacao(autenticado, id);
        exigirStatus(empenho, StatusEmpenho.PENDENTE, "Só é possível confirmar um empenho pendente.");
        List<ItemEmpenhoJpaEntity> linhas = itensEmpenho.buscarPorEmpenhoParaAtualizacao(id);
        Map<Long, ItemMaterialJpaEntity> itens = bloquearItensDoEmpenho(autenticado, linhas);
        RecebedorJpaEntity recebedor = recebedores.buscarPorIdDoSetorParaAtualizacao(
                        empenho.getRecebedor().getId(), autenticado.setor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recebedor não encontrado neste setor."));

        if (!recebedor.verificarPin(requisicao.pin(), Instant.now())) {
            return new ConfirmacaoEmpenhoResponse(false, "Não foi possível confirmar o recebedor.", resposta(empenho));
        }

        for (ItemEmpenhoJpaEntity linha : linhas) {
            ItemMaterialJpaEntity item = itens.get(linha.getItemMaterial().getId());
            if (item == null || item.getStatus() != StatusItem.DISPONIVEL
                    || item.getEmpenhoPendente() != empenho || !item.getMaterial().isAtivo()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Um item reservado não está mais disponível para confirmação.");
            }
        }

        UsuarioJpaEntity operador = buscarOperador(autenticado);
        for (ItemEmpenhoJpaEntity linha : linhas) {
            itens.get(linha.getItemMaterial().getId()).confirmarEmpenho(empenho);
            linha.confirmar();
        }
        empenho.confirmarPorPin(operador, requisicao.justificativa());
        return new ConfirmacaoEmpenhoResponse(true, "Empenho confirmado.", resposta(empenho));
    }

    @Transactional
    public void cancelar(UsuarioAutenticado autenticado, long id) {
        EmpenhoJpaEntity empenho = buscarParaAtualizacao(autenticado, id);
        exigirStatus(empenho, StatusEmpenho.PENDENTE, "Só é possível cancelar um empenho pendente.");
        List<ItemEmpenhoJpaEntity> linhas = itensEmpenho.buscarPorEmpenhoParaAtualizacao(id);
        Map<Long, ItemMaterialJpaEntity> itens = bloquearItensDoEmpenho(autenticado, linhas);
        for (ItemEmpenhoJpaEntity linha : linhas) {
            ItemMaterialJpaEntity item = itens.get(linha.getItemMaterial().getId());
            if (item == null || item.getEmpenhoPendente() != empenho) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A reserva de um item não corresponde a este empenho.");
            }
        }
        for (ItemEmpenhoJpaEntity linha : linhas) {
            itens.get(linha.getItemMaterial().getId()).cancelarReserva(empenho);
        }
        empenho.cancelar(buscarOperador(autenticado));
    }

    @Transactional
    public DevolucaoResponse registrarDevolucao(
            UsuarioAutenticado autenticado, long empenhoId, RegistrarDevolucaoRequest requisicao) {
        EmpenhoJpaEntity empenho = buscarParaAtualizacao(autenticado, empenhoId);
        if (empenho.getStatus() != StatusEmpenho.ABERTO
                && empenho.getStatus() != StatusEmpenho.DEVOLVIDO_PARCIALMENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O empenho não possui itens aguardando devolução.");
        }

        Set<Long> idsLinhas = new HashSet<>();
        for (ItemDevolucaoRequest item : requisicao.itens()) {
            if (!idsLinhas.add(item.itemEmpenhoId())) {
                throw new IllegalArgumentException("Um item não pode ser devolvido duas vezes na mesma operação.");
            }
        }

        List<ItemEmpenhoJpaEntity> linhasEmpenho = itensEmpenho.buscarPorEmpenhoParaAtualizacao(empenhoId);
        Map<Long, ItemEmpenhoJpaEntity> linhasPorId = new HashMap<>();
        for (ItemEmpenhoJpaEntity linha : linhasEmpenho) linhasPorId.put(linha.getId(), linha);
        for (Long idLinha : idsLinhas) {
            if (!linhasPorId.containsKey(idLinha)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Um ou mais itens não pertencem a este empenho.");
            }
            if (linhasPorId.get(idLinha).getSituacao() != SituacaoItemEmpenho.EMPENHADO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Um ou mais itens já foram devolvidos ou extraviados.");
            }
        }

        RecebedorJpaEntity devolvidoPor = recebedores.findByIdAndSetor(requisicao.devolvidoPorId(), autenticado.setor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recebedor não encontrado neste setor."));
        if (!devolvidoPor.isAtivo() && !devolvidoPor.getId().equals(empenho.getRecebedor().getId())) {
            throw new IllegalArgumentException("Um recebedor inativo só pode devolver os itens do próprio empenho.");
        }

        List<ItemMaterialJpaEntity> itensBloqueados = itensMaterial.buscarPorIdsDoSetorParaAtualizacao(
                linhasEmpenho.stream().filter(linha -> idsLinhas.contains(linha.getId()))
                        .map(linha -> linha.getItemMaterial().getId()).toList(),
                autenticado.setor());
        Map<Long, ItemMaterialJpaEntity> materiaisPorId = new HashMap<>();
        for (ItemMaterialJpaEntity item : itensBloqueados) materiaisPorId.put(item.getId(), item);

        UsuarioJpaEntity operador = buscarOperador(autenticado);
        DevolucaoJpaEntity devolucao = new DevolucaoJpaEntity(
                empenho, operador, devolvidoPor, requisicao.observacao());
        for (ItemDevolucaoRequest requisicaoItem : requisicao.itens()) {
            ItemEmpenhoJpaEntity linha = linhasPorId.get(requisicaoItem.itemEmpenhoId());
            ItemMaterialJpaEntity item = materiaisPorId.get(linha.getItemMaterial().getId());
            if (item == null || item.getStatus() != StatusItem.EMPENHADO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "O item não está emprestado para devolução.");
            }
            item.registrarDevolucao(requisicaoItem.condicao());
            linha.devolver();
            devolucao.adicionarItem(new ItemDevolucaoJpaEntity(
                    devolucao, linha, requisicaoItem.condicao(), requisicaoItem.observacao()));
        }
        empenho.recalcularStatus();
        devolucoes.save(devolucao);
        return resposta(devolucao);
    }

    @Transactional(readOnly = true)
    public List<DevolucaoResponse> listarDevolucoes(UsuarioAutenticado usuario, long empenhoId) {
        empenhos.findByIdAndSetor(empenhoId, usuario.setor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empenho não encontrado neste setor."));
        return devolucoes.findAllByEmpenho_IdOrderByDataHoraAsc(empenhoId).stream()
                .map(MovimentacaoApplicationService::resposta)
                .toList();
    }

    @Transactional
    public void registrarExtravio(UsuarioAutenticado autenticado, long empenhoId, long itemEmpenhoId) {
        EmpenhoJpaEntity empenho = buscarParaAtualizacao(autenticado, empenhoId);
        if (empenho.getStatus() == StatusEmpenho.PENDENTE || empenho.getStatus() == StatusEmpenho.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Só é possível registrar extravio de um empenho confirmado.");
        }

        List<ItemEmpenhoJpaEntity> linhas = itensEmpenho.buscarPorEmpenhoParaAtualizacao(empenhoId);
        ItemEmpenhoJpaEntity linha = linhas.stream().filter(item -> item.getId() == itemEmpenhoId).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado neste empenho."));
        Map<Long, ItemMaterialJpaEntity> itens = bloquearItensDoEmpenho(
                autenticado, List.of(linha));
        ItemMaterialJpaEntity item = itens.get(linha.getItemMaterial().getId());
        if (item == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado neste estoque.");
        UsuarioJpaEntity administrador = buscarAdministradorEstoque(autenticado);
        item.marcarExtraviado();
        linha.marcarExtraviado(administrador, Instant.now());
        empenho.recalcularStatus();
    }

    private Page<EmpenhoResponse> listarPorStatus(
            UsuarioAutenticado usuario, int pagina, int tamanho, Collection<StatusEmpenho> status) {
        Pageable pageable = paginacao(pagina, tamanho);
        return empenhos.findAllBySetorAndStatusInOrderByDataHoraDesc(usuario.setor(), status, pageable)
                .map(this::resposta);
    }

    private Map<Long, ItemMaterialJpaEntity> bloquearItensDoEmpenho(
            UsuarioAutenticado usuario, List<ItemEmpenhoJpaEntity> linhas) {
        List<Long> ids = linhas.stream().map(linha -> linha.getItemMaterial().getId()).distinct().toList();
        List<ItemMaterialJpaEntity> itens = itensMaterial.buscarPorIdsDoSetorParaAtualizacao(ids, usuario.setor());
        Map<Long, ItemMaterialJpaEntity> porId = new HashMap<>();
        for (ItemMaterialJpaEntity item : itens) porId.put(item.getId(), item);
        return porId;
    }

    private EmpenhoJpaEntity buscarParaAtualizacao(UsuarioAutenticado usuario, long id) {
        return empenhos.buscarPorIdDoSetorParaAtualizacao(id, usuario.setor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empenho não encontrado neste setor."));
    }

    private UsuarioJpaEntity buscarOperador(UsuarioAutenticado usuario) {
        UsuarioJpaEntity operador = usuarios.findById(usuario.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação inválida."));
        if (!operador.isAtivo() || operador.getPerfil() != PerfilUsuario.OPERADOR_ESTOQUE
                || !operador.getSetor().equals(usuario.setor())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem permissão para registrar movimentações.");
        }
        return operador;
    }

    private UsuarioJpaEntity buscarAdministradorEstoque(UsuarioAutenticado usuario) {
        UsuarioJpaEntity administrador = usuarios.findById(usuario.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação inválida."));
        if (!administrador.isAtivo() || administrador.getPerfil() != PerfilUsuario.ADMINISTRADOR_ESTOQUE
                || !administrador.getSetor().equals(usuario.setor())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem permissão para registrar extravios.");
        }
        return administrador;
    }

    private static Pageable paginacao(int pagina, int tamanho) {
        if (pagina < 0) throw new IllegalArgumentException("A página não pode ser negativa.");
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException("O tamanho da página deve estar entre 1 e " + TAMANHO_MAXIMO_PAGINA + ".");
        }
        return PageRequest.of(pagina, tamanho);
    }

    private static void exigirStatus(EmpenhoJpaEntity empenho, StatusEmpenho esperado, String mensagem) {
        if (empenho.getStatus() != esperado) throw new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private EmpenhoResponse resposta(EmpenhoJpaEntity empenho) {
        List<ItemEmpenhoJpaEntity> itens = empenho.getId() == null
                ? empenho.getItens()
                : itensEmpenho.findAllByEmpenho_IdOrderById(empenho.getId());
        List<ItemEmpenhoResponse> itensResposta = itens.stream().map(MovimentacaoApplicationService::resposta).toList();
        return new EmpenhoResponse(
                empenho.getId(),
                empenho.getStatus(),
                empenho.getDataHora(),
                empenho.getObservacao(),
                empenho.getEquipe().getId(),
                empenho.getEquipe().getNome(),
                empenho.getRecebedor().getId(),
                empenho.getRecebedor().getNome(),
                empenho.getOperador().getNome(),
                empenho.getConfirmadoEm(),
                empenho.isConfirmadoPorPin() ? "PIN" : null,
                empenho.getJustificativaPin(),
                itensResposta);
    }

    private static ItemEmpenhoResponse resposta(ItemEmpenhoJpaEntity linha) {
        ItemMaterialJpaEntity item = linha.getItemMaterial();
        return new ItemEmpenhoResponse(
                linha.getId(),
                item.getId(),
                item.getNumeroPatrimonio(),
                item.getMaterial().getCodigo(),
                item.getMaterial().getNome(),
                item.getStatus(),
                linha.getSituacao(),
                linha.getExtraviadoEm());
    }

    private static DevolucaoResponse resposta(DevolucaoJpaEntity devolucao) {
        List<ItemDevolucaoResponse> itens = devolucao.getItens().stream()
                .map(item -> new ItemDevolucaoResponse(
                        item.getItemEmpenho().getId(),
                        item.getItemEmpenho().getItemMaterial().getNumeroPatrimonio(),
                        item.getCondicao(),
                        item.getObservacao()))
                .toList();
        return new DevolucaoResponse(
                devolucao.getId(),
                devolucao.getEmpenho().getId(),
                devolucao.getDataHora(),
                devolucao.getObservacao(),
                devolucao.getRegistradoPor().getNome(),
                devolucao.getDevolvidoPor().getNome(),
                itens);
    }
}
