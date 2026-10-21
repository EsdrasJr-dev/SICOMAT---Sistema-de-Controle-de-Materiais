package br.com.sicomat.usuario;

public class AdministradorEstoque extends Usuario {

    private String setor;

    public AdministradorEstoque(String nome, String matricula, String senhaHash, String setor) {
        super(nome, matricula, senhaHash);
        this.setor = setor;
    }

    public String getSetor() {
        return setor;
    }

    @Override
    public String getPerfil() {
        return "ADMINISTRADOR_ESTOQUE";
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
