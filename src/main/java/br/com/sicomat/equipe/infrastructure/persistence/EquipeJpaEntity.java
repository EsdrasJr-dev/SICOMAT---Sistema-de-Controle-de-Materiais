package br.com.sicomat.equipe.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "equipe")
public class EquipeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String setor;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private boolean ativo = true;

    protected EquipeJpaEntity() {
    }

    public EquipeJpaEntity(String setor, String nome) {
        this.setor = textoObrigatorio(setor, "setor");
        this.nome = textoObrigatorio(nome, "nome da equipe");
    }

    public Long getId() { return id; }
    public String getSetor() { return setor; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
    public void inativar() { ativo = false; }

    private static String textoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("O campo " + campo + " é obrigatório.");
        return valor.trim();
    }
}
