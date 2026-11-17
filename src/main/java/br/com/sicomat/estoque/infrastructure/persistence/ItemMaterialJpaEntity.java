package br.com.sicomat.estoque.infrastructure.persistence;

import br.com.sicomat.estoque.StatusItem;
import br.com.sicomat.movimentacao.CondicaoItem;
import br.com.sicomat.movimentacao.infrastructure.persistence.EmpenhoJpaEntity;
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
import jakarta.persistence.Version;
import java.util.Objects;

@Entity
@Table(name = "item_material")
public class ItemMaterialJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private MaterialJpaEntity material;

    @Column(nullable = false, length = 80)
    private String setor;

    @Column(name = "numero_patrimonio", nullable = false, length = 80)
    private String numeroPatrimonio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusItem status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empenho_pendente_id")
    private EmpenhoJpaEntity empenhoPendente;

    @Version
    @Column(nullable = false)
    private long versao;

    protected ItemMaterialJpaEntity() {
    }

    public ItemMaterialJpaEntity(MaterialJpaEntity material, String numeroPatrimonio, StatusItem status) {
        this.material = Objects.requireNonNull(material, "O material é obrigatório.");
        this.setor = material.getSetor();
        this.numeroPatrimonio = numeroPatrimonio;
        this.status = status;
    }

    public Long getId() { return id; }
    public MaterialJpaEntity getMaterial() { return material; }
    public String getSetor() { return setor; }
    public String getNumeroPatrimonio() { return numeroPatrimonio; }
    public StatusItem getStatus() { return status; }
    public EmpenhoJpaEntity getEmpenhoPendente() { return empenhoPendente; }
    public boolean isReservado() { return empenhoPendente != null; }
    public void setStatus(StatusItem status) { this.status = status; }

    public void reservarPara(EmpenhoJpaEntity empenho) {
        Objects.requireNonNull(empenho, "O empenho é obrigatório.");
        if (status != StatusItem.DISPONIVEL || empenhoPendente != null || !material.isAtivo()) {
            throw new IllegalStateException("O item não está disponível para reserva.");
        }
        empenhoPendente = empenho;
    }

    public void cancelarReserva(EmpenhoJpaEntity empenho) {
        if (empenhoPendente != empenho) {
            throw new IllegalStateException("O item não está reservado por este empenho.");
        }
        empenhoPendente = null;
        if (!material.isAtivo() && status == StatusItem.DISPONIVEL) status = StatusItem.INATIVO;
    }

    public void confirmarEmpenho(EmpenhoJpaEntity empenho) {
        if (empenhoPendente != empenho || status != StatusItem.DISPONIVEL || !material.isAtivo()) {
            throw new IllegalStateException("O item não está reservado por um empenho válido.");
        }
        empenhoPendente = null;
        status = StatusItem.EMPENHADO;
    }

    public void registrarDevolucao(CondicaoItem condicao) {
        Objects.requireNonNull(condicao, "A condição do item é obrigatória.");
        if (status != StatusItem.EMPENHADO) {
            throw new IllegalStateException("Só é possível devolver um item empenhado.");
        }
        status = condicao == CondicaoItem.AVARIADO
                ? StatusItem.AVARIADO
                : (material.isAtivo() ? StatusItem.DISPONIVEL : StatusItem.INATIVO);
    }

    public void marcarExtraviado() {
        if (status != StatusItem.EMPENHADO) {
            throw new IllegalStateException("Só é possível registrar extravio de um item empenhado.");
        }
        status = StatusItem.EXTRAVIADO;
    }
}
