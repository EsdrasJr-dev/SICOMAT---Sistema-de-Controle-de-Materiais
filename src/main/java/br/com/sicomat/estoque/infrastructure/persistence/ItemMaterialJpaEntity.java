package br.com.sicomat.estoque.infrastructure.persistence;

import br.com.sicomat.estoque.StatusItem;
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

    protected ItemMaterialJpaEntity() {
    }

    public ItemMaterialJpaEntity(MaterialJpaEntity material, String numeroPatrimonio, StatusItem status) {
        this.material = material;
        this.numeroPatrimonio = numeroPatrimonio;
        this.status = status;
    }

    public Long getId() { return id; }
    public MaterialJpaEntity getMaterial() { return material; }
    public String getSetor() { return setor; }
    public String getNumeroPatrimonio() { return numeroPatrimonio; }
    public StatusItem getStatus() { return status; }
    public void setStatus(StatusItem status) { this.status = status; }
}