package br.com.sicomat.usuario;

import br.com.sicomat.seguranca.CodificadorSenha;
import br.com.sicomat.seguranca.ControleTentativasAutenticacao;

/** senhaHash deve ser gerado por CodificadorSenha.codificar, nunca armazenar a senha em texto puro. */
public abstract class Usuario {

    private final String nome;
    private final String matricula;
    private String senhaHash;
    private boolean ativo = true;
    private final ControleTentativasAutenticacao tentativas = new ControleTentativasAutenticacao();

    protected Usuario(String nome, String matricula, String senhaHash) {
        this.nome = textoObrigatorio(nome, "nome");
        this.matricula = textoObrigatorio(matricula, "matrícula");
        if (!CodificadorSenha.formatoValido(senhaHash)) {
            throw new IllegalArgumentException("A senha deve ser armazenada em formato de hash PBKDF2 válido.");
        }
        this.senhaHash = senhaHash;
    }

    public String getNome() { return nome; }
    public String getMatricula() { return matricula; }
    public synchronized boolean isAtivo() { return ativo; }

    public synchronized boolean autenticar(String senha) {
        if (!ativo) return false;
        tentativas.exigirTentativaPermitida();
        if (CodificadorSenha.verificar(senha, senhaHash)) {
            tentativas.registrarSucesso();
            return true;
        }
        tentativas.registrarFalha();
        return false;
    }

    synchronized void inativar() { ativo = false; }
    synchronized void redefinirSenha(String senha) {
        senhaHash = CodificadorSenha.codificar(senha);
        tentativas.redefinir();
    }

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
