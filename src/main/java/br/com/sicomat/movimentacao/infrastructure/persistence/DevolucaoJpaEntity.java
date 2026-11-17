package br.com.sicomat.movimentacao.infrastructure.persistence;

import br.com.sicomat.equipe.infrastructure.persistence.RecebedorJpaEntity;
import br.com.sicomat.usuario.infrastructure.persistence.UsuarioJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "devolucao")
public class DevolucaoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empenho_id", nullable = false)
    private EmpenhoJpaEntity empenho;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false)
    private UsuarioJpaEntity registradoPor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolvido_por", nullable = false)
    private RecebedorJpaEntity devolvidoPor;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora = Instant.now();

    @Column(nullable = false, length = 500)
    private String observacao;

    @OneToMany(mappedBy = "devolucao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ItemDevolucaoJpaEntity> itens = new ArrayList<>();

    protected DevolucaoJpaEntity() {
    }

    public DevolucaoJpaEntity(EmpenhoJpaEntity empenho, UsuarioJpaEntity registradoPor,
                              RecebedorJpaEntity devolvidoPor, String observacao) {
        this.empenho = Objects.requireNonNull(empenho, "O empenho é obrigatório.");
        this.registradoPor = Objects.requireNonNull(registradoPor, "O operador é obrigatório.");
        this.devolvidoPor = Objects.requireNonNull(devolvidoPor, "A pessoa que devolveu é obrigatória.");
        this.observacao = observacao == null ? "" : observacao.trim();
    }

    public Long getId() { return id; }
    public EmpenhoJpaEntity getEmpenho() { return empenho; }
    public UsuarioJpaEntity getRegistradoPor() { return registradoPor; }
    public RecebedorJpaEntity getDevolvidoPor() { return devolvidoPor; }
    public Instant getDataHora() { return dataHora; }
    public String getObservacao() { return observacao; }
    public List<ItemDevolucaoJpaEntity> getItens() { return Collections.unmodifiableList(itens); }

    public void adicionarItem(ItemDevolucaoJpaEntity item) {
        itens.add(Objects.requireNonNull(item, "O item de devolução é obrigatório."));
    }
}
