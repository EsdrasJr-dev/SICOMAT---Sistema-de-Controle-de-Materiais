package br.com.sicomat.usuario;

import br.com.sicomat.seguranca.CodificadorSenha;

/** senhaHash deve ser gerado por CodificadorSenha.codificar, nunca armazenar a senha em texto puro. */
public abstract class Usuario {

    private final String nome;
    private final String matricula;
    private String senhaHash;
    private boolean ativo = true;

    protected Usuario(String nome, String matricula, String senhaHash) {
        this.nome = textoObrigatorio(nome, "nome");
        this.matricula = textoObrigatorio(matricula, "matrícula");
        this.senhaHash = textoObrigatorio(senhaHash, "hash da senha");
    }

    public String getNome() { return nome; }
    public String getMatricula() { return matricula; }
    public synchronized boolean isAtivo() { return ativo; }

    public synchronized boolean autenticar(String senha) {
        return ativo && CodificadorSenha.verificar(senha, senhaHash);
    }

    synchronized void inativar() { ativo = false; }
    synchronized void redefinirSenha(String senha) { senhaHash = CodificadorSenha.codificar(senha); }

    public abstract String getPerfil();
    public abstract boolean podeGerenciarMaterial();
    public abstract boolean podeRegistrarMovimentacao();

    protected static String textoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo " + campo + " é obrigatório.");
        }
        return valor.trim();
    }
}

