package br.com.sicomat.usuario;

public class OperadorEstoque extends Usuario {

    private String turno;

    public OperadorEstoque(String nome, String matricula, String senhaHash, String turno) {
        super(nome, matricula, senhaHash);
        this.turno = turno;
    }

    public String getTurno() {
        return turno;
    }

    @Override
    public String getPerfil() {
        return "OPERADOR_ESTOQUE";
    }

    @Override
    public boolean podeGerenciarMaterial() {
        // TODO: definir a permissão deste perfil.
        return false;
    }

    @Override
    public boolean podeRegistrarMovimentacao() {
        // TODO: definir a permissão deste perfil.
        return false;
    }
}
