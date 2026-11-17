package br.com.sicomat.movimentacao.infrastructure.persistence;

import br.com.sicomat.movimentacao.CondicaoItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "item_devolucao")
public class ItemDevolucaoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolucao_id", nullable = false)
    private DevolucaoJpaEntity devolucao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_empenho_id", nullable = false, unique = true)
    private ItemEmpenhoJpaEntity itemEmpenho;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CondicaoItem condicao;

    @Column(nullable = false, length = 500)
    private String observacao;

    protected ItemDevolucaoJpaEntity() {
    }

    public ItemDevolucaoJpaEntity(DevolucaoJpaEntity devolucao, ItemEmpenhoJpaEntity itemEmpenho,
                                  CondicaoItem condicao, String observacao) {
        this.devolucao = Objects.requireNonNull(devolucao, "A devolução é obrigatória.");
        this.itemEmpenho = Objects.requireNonNull(itemEmpenho, "O item do empenho é obrigatório.");
        this.condicao = Objects.requireNonNull(condicao, "A condição do item é obrigatória.");
        this.observacao = observacao == null ? "" : observacao.trim();
        if (condicao == CondicaoItem.AVARIADO && this.observacao.isBlank()) {
            throw new IllegalArgumentException("Descreva a avaria observada.");
        }
    }

    public Long getId() { return id; }
    public DevolucaoJpaEntity getDevolucao() { return devolucao; }
    public ItemEmpenhoJpaEntity getItemEmpenho() { return itemEmpenho; }
    public CondicaoItem getCondicao() { return condicao; }
    public String getObservacao() { return observacao; }
}
