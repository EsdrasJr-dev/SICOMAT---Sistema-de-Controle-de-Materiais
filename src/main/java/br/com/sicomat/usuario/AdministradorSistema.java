package br.com.sicomat.usuario;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AdministradorSistema extends Usuario {

    private final String setor;
    private final List<Usuario> usuarios = new ArrayList<>();

    public AdministradorSistema(String nome, String matricula, String senhaHash, String setor) {
        this(nome, matricula, senhaHash, setor, Collections.emptyList());
    }

    /** Recebe as contas carregadas pelo repositório quando a aplicação tiver persistência. */
    public AdministradorSistema(String nome, String matricula, String senhaHash, String setor,
                                Collection<Usuario> usuariosExistentes) {
        super(nome, matricula, senhaHash);
        if (setor == null || setor.isBlank()) throw new IllegalArgumentException("O setor é obrigatório.");
        this.setor = setor.trim();
        usuarios.add(this);
        if (usuariosExistentes != null) {
            for (Usuario usuario : usuariosExistentes) {
                if (usuario != null && !usuarios.contains(usuario)) usuarios.add(usuario);
            }
        }
    }

    public String getSetor() { return setor; }

    public synchronized void cadastrarUsuario(Usuario usuario) {
        exigirAdministradorAtivo();
        if (!(usuario instanceof AdministradorEstoque) && !(usuario instanceof OperadorEstoque)) {
            throw new IllegalArgumentException("O administrador do sistema só pode cadastrar administrador ou operador do estoque.");
        }
        if (usuarios.stream().anyMatch(cadastrado -> cadastrado.getMatricula().equalsIgnoreCase(usuario.getMatricula()))) {
            throw new IllegalArgumentException("Já existe um usuário com essa matrícula.");
        }
        usuarios.add(usuario);
    }

    public synchronized void inativarUsuario(Usuario usuario) {
        exigirAdministradorAtivo();
        exigirUsuarioCadastrado(usuario);
        if (usuario instanceof AdministradorSistema) {
            long administradoresAtivos = usuarios.stream()
                    .filter(AdministradorSistema.class::isInstance)
                    .filter(Usuario::isAtivo)
                    .count();
            if (administradoresAtivos <= 1) {
                throw new IllegalStateException("O último administrador do sistema ativo não pode ser inativado.");
            }
        }
        usuario.inativar();
    }

    public synchronized void redefinirSenha(Usuario usuario, String novaSenha) {
        exigirAdministradorAtivo();
        exigirUsuarioCadastrado(usuario);
        usuario.redefinirSenha(novaSenha);
    }

    public synchronized List<Usuario> listarUsuarios() {
        return Collections.unmodifiableList(new ArrayList<>(usuarios));
    }

    private void exigirAdministradorAtivo() {
        if (!isAtivo()) throw new IllegalStateException("A conta do administrador do sistema está inativa.");
    }

    private void exigirUsuarioCadastrado(Usuario usuario) {
        if (usuario == null || !usuarios.contains(usuario)) throw new IllegalArgumentException("O usuário não pertence a este cadastro.");
    }

    @Override public String getPerfil() { return "ADMINISTRADOR_SISTEMA"; }
    @Override public boolean podeGerenciarMaterial() { return false; }
    @Override public boolean podeRegistrarMovimentacao() { return false; }
}
