package br.com.sicomat.equipe;

public class Equipe {

    private final String nome;
    private final String setor;

    public Equipe(String nome, String setor) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("O nome da equipe é obrigatório.");
        if (setor == null || setor.isBlank()) throw new IllegalArgumentException("O setor da equipe é obrigatório.");
        this.nome = nome.trim();
        this.setor = setor.trim();
    }

    public String getNome() { return nome; }
    public String getSetor() { return setor; }
}
