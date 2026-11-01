package br.com.sicomat.estoque.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "material")
public class MaterialJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String setor;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private boolean ativo = true;

    protected MaterialJpaEntity() {
    }

    public MaterialJpaEntity(String setor, String codigo, String nome) {
        this.setor = setor;
        this.codigo = codigo;
        this.nome = nome;
        this.ativo = true;
    }

    public Long getId() { return id; }
    public String getSetor() { return setor; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }

    public void editar(String codigo, String nome) {
        if (!ativo) throw new IllegalStateException("Não é possível editar um material inativo.");
        this.codigo = codigo;
        this.nome = nome;
    }

    public void inativar() { ativo = false; }
}