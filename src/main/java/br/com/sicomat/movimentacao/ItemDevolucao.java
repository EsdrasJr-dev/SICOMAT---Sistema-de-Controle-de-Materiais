package br.com.sicomat.movimentacao;

import java.util.Objects;

public class ItemDevolucao {

    private final CondicaoItem condicao;
    private final String observacao;
    private final ItemEmpenho itemEmpenho;
    private ItemEmpenho itemSubstituto;

    public ItemDevolucao(ItemEmpenho itemEmpenho, CondicaoItem condicao, String observacao) {
        this.itemEmpenho = Objects.requireNonNull(itemEmpenho, "O item do empenho é obrigatório.");
        this.condicao = Objects.requireNonNull(condicao, "A condição do item é obrigatória.");
        this.observacao = observacao == null ? "" : observacao.trim();
        if (condicao == CondicaoItem.AVARIADO && this.observacao.isBlank()) {
            throw new IllegalArgumentException("Descreva a avaria observada.");
        }
    }

    public CondicaoItem getCondicao() { return condicao; }
    public String getObservacao() { return observacao; }
    public ItemEmpenho getItemEmpenho() { return itemEmpenho; }
    public synchronized ItemEmpenho getItemSubstituto() { return itemSubstituto; }
    public boolean estaAvariado() { return condicao == CondicaoItem.AVARIADO; }

    public synchronized void vincularSubstituto(ItemEmpenho substituto) {
        Objects.requireNonNull(substituto, "O item substituto é obrigatório.");
        if (substituto == itemEmpenho || substituto.getEmpenho() == itemEmpenho.getEmpenho()) {
            throw new IllegalArgumentException("O item substituto deve pertencer a outro empenho.");
        }
        if (!substituto.estaPendente()) throw new IllegalArgumentException("O item substituto precisa estar empenhado.");
        if (substituto.getEmpenho().getRecebedor() != itemEmpenho.getEmpenho().getRecebedor()) {
            throw new IllegalArgumentException("A substituição deve permanecer vinculada ao mesmo recebedor.");
        }
        itemSubstituto = substituto;
    }

    void validarRegistro() {
        if (!itemEmpenho.estaPendente()) throw new IllegalStateException("Este item já foi devolvido ou extraviado.");
    }

    void registrar() { itemEmpenho.marcarDevolvido(estaAvariado()); }
}
