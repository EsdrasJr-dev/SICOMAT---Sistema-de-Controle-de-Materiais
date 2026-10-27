package br.com.sicomat.movimentacao;

import br.com.sicomat.equipe.Recebedor;
import br.com.sicomat.usuario.OperadorEstoque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Devolucao extends Movimentacao {

    private final Empenho empenho;
    private final Recebedor devolvidoPor;
    private final List<ItemDevolucao> itens;
    private boolean registrada;
    private boolean parcial;

    public Devolucao(Empenho empenho, Recebedor devolvidoPor, OperadorEstoque operador,
                     List<ItemDevolucao> itens, String observacao) {
        super(operador, observacao);
        this.empenho = Objects.requireNonNull(empenho, "O empenho original é obrigatório.");
        this.devolvidoPor = Objects.requireNonNull(devolvidoPor, "A pessoa que entregou os itens é obrigatória.");
        operador.validarAcesso(empenho);
        if (itens == null || itens.isEmpty()) throw new IllegalArgumentException("A devolução precisa conter ao menos um item.");
        Set<ItemEmpenho> unicos = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ItemDevolucao> copia = new ArrayList<>();
        for (ItemDevolucao item : itens) {
            Objects.requireNonNull(item, "O item de devolução não pode ser nulo.");
            if (item.getItemEmpenho().getEmpenho() != empenho) throw new IllegalArgumentException("Uma devolução só pode conter itens do mesmo empenho.");
            if (!unicos.add(item.getItemEmpenho())) throw new IllegalArgumentException("Um item não pode ser devolvido duas vezes na mesma operação.");
            copia.add(item);
        }
        this.itens = copia;
    }

    public Empenho getEmpenho() { return empenho; }
    public Recebedor getDevolvidoPor() { return devolvidoPor; }
    public synchronized List<ItemDevolucao> getItens() { return Collections.unmodifiableList(new ArrayList<>(itens)); }
    public synchronized boolean isRegistrada() { return registrada; }

    @Override public synchronized void registrar() {
        synchronized (empenho) {
            if (registrada) throw new IllegalStateException("Esta devolução já foi registrada.");
            if (!empenho.emAberto()) throw new IllegalStateException("O empenho não possui itens aguardando devolução.");
            if (!devolvidoPor.isAtivo() && devolvidoPor != empenho.getRecebedor()) {
                throw new IllegalStateException("Uma pessoa inativa só pode devolver itens do próprio empenho.");
            }
            for (ItemDevolucao item : itens) item.validarRegistro();
            for (ItemDevolucao item : itens) item.registrar();
            parcial = !empenho.getItensPendentes().isEmpty();
            registrada = true;
            empenho.adicionarDevolucao(this);
        }
    }

    public synchronized boolean ehParcial() {
        return registrada && parcial;
    }
}
