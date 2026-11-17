package br.com.sicomat.movimentacao.infrastructure.persistence;

import br.com.sicomat.estoque.infrastructure.persistence.ItemMaterialJpaEntity;
import br.com.sicomat.movimentacao.SituacaoItemEmpenho;
import br.com.sicomat.usuario.infrastructure.persistence.UsuarioJpaEntity;
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
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "item_empenho")
public class ItemEmpenhoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empenho_id", nullable = false)
    private EmpenhoJpaEntity empenho;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_material_id", nullable = false)
    private ItemMaterialJpaEntity itemMaterial;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SituacaoItemEmpenho situacao;

    @Column(name = "extraviado_em")
    private Instant extraviadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extravio_registrado_por")
    private UsuarioJpaEntity extravioRegistradoPor;

    protected ItemEmpenhoJpaEntity() {
    }

    public ItemEmpenhoJpaEntity(EmpenhoJpaEntity empenho, ItemMaterialJpaEntity itemMaterial) {
        this.empenho = Objects.requireNonNull(empenho, "O empenho é obrigatório.");
        this.itemMaterial = Objects.requireNonNull(itemMaterial, "O item físico é obrigatório.");
    }

    public Long getId() { return id; }
    public EmpenhoJpaEntity getEmpenho() { return empenho; }
    public ItemMaterialJpaEntity getItemMaterial() { return itemMaterial; }
    public SituacaoItemEmpenho getSituacao() { return situacao; }
    public Instant getExtraviadoEm() { return extraviadoEm; }
    public UsuarioJpaEntity getExtravioRegistradoPor() { return extravioRegistradoPor; }

    public void confirmar() { situacao = SituacaoItemEmpenho.EMPENHADO; }

    public void devolver() {
        if (situacao != SituacaoItemEmpenho.EMPENHADO) throw new IllegalStateException("O item não está pendente de devolução.");
        situacao = SituacaoItemEmpenho.DEVOLVIDO;
    }

    public void marcarExtraviado(UsuarioJpaEntity administrador, Instant agora) {
        if (situacao != SituacaoItemEmpenho.EMPENHADO) throw new IllegalStateException("O item não está empenhado.");
        situacao = SituacaoItemEmpenho.EXTRAVIADO;
        extraviadoEm = Objects.requireNonNull(agora, "A data do extravio é obrigatória.");
        extravioRegistradoPor = Objects.requireNonNull(administrador, "O administrador é obrigatório.");
    }
}
