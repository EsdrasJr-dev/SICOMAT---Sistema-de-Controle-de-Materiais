package br.com.sicomat.usuario;

public abstract class Usuario {

    private String nome;
    private String matricula;
    private String senhaHash;

    protected Usuario(String nome, String matricula, String senhaHash) {
        this.nome = nome;
        this.matricula = matricula;
        this.senhaHash = senhaHash;
    }

    public String getNome() {
        return nome;
    }

    public String getMatricula() {
        return matricula;
    }

    public boolean autenticar(String senha) {
        // TODO: verificar a senha digitada usando uma biblioteca de hash de senhas.
        return false;
    }

    public abstract String getPerfil();

    public abstract boolean podeGerenciarMaterial();

    public abstract boolean podeRegistrarMovimentacao();
}
