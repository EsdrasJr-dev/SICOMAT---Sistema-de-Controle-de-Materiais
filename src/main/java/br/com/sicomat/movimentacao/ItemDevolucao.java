package br.com.sicomat.movimentacao;

import java.util.Objects;

public class ItemDevolucao {

    private final CondicaoItem condicao;
    private final String observacao;
    private final ItemEmpenho itemEmpenho;

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
    public boolean estaAvariado() { return condicao == CondicaoItem.AVARIADO; }

    void validarRegistro() {
        if (!itemEmpenho.estaPendente() || !itemEmpenho.getItemMaterial().estaEmpenhado()) {
            throw new IllegalStateException("O item não está empenhado ou já foi devolvido ou extraviado.");
        }
    }

    void registrar() { itemEmpenho.marcarDevolvido(estaAvariado()); }
}
