package br.com.sicomat.usuario;

import br.com.sicomat.estoque.Estoque;
import br.com.sicomat.estoque.ItemMaterial;
import br.com.sicomat.estoque.Material;
import br.com.sicomat.equipe.Recebedor;
import br.com.sicomat.movimentacao.Empenho;
import br.com.sicomat.movimentacao.ItemEmpenho;
import br.com.sicomat.seguranca.VerificadorFacial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AdministradorEstoque extends Usuario {

    private final String setor;
    private final Estoque estoque;
    private final VerificadorFacial verificadorFacial;
    private final List<Recebedor> recebedores = new ArrayList<>();

    public AdministradorEstoque(String nome, String matricula, String senhaHash, String setor) {
        this(nome, matricula, senhaHash, setor, new Estoque(setor), null);
    }

    public AdministradorEstoque(String nome, String matricula, String senhaHash, String setor,
                                Estoque estoque, VerificadorFacial verificadorFacial) {
        super(nome, matricula, senhaHash);
        this.setor = textoObrigatorio(setor, "setor");
        this.estoque = Objects.requireNonNull(estoque, "O estoque é obrigatório.");
        this.verificadorFacial = verificadorFacial;
    }

    public String getSetor() { return setor; }
    public Estoque getEstoque() { return estoque; }
    public synchronized List<Recebedor> listarRecebedores() {
        return Collections.unmodifiableList(new ArrayList<>(recebedores));
    }

    public void cadastrarMaterial(Material material) {
        exigirAtivo();
        estoque.adicionarMaterial(material);
    }

    /** Valida um material já editado antes de confirmar a alteração no cadastro em memória. */
    public void editarMaterial(Material material) {
        exigirAtivo();
        if (!estoque.contem(material)) throw new IllegalArgumentException("O material não pertence a este estoque.");
        estoque.editarMaterial(material, material.getCodigo(), material.getNome());
    }

    public void editarMaterial(Material material, String novoCodigo, String novoNome) {
        exigirAtivo();
        if (!estoque.contem(material)) throw new IllegalArgumentException("O material não pertence a este estoque.");
        estoque.editarMaterial(material, novoCodigo, novoNome);
    }

    public void inativarMaterial(Material material) {
        exigirAtivo();
        if (!estoque.contem(material)) throw new IllegalArgumentException("O material não pertence a este estoque.");
        material.inativar();
    }

    public void cadastrarItem(Material material, String patrimonio) {
        exigirAtivo();
        estoque.adicionarItem(material, patrimonio);
    }

    public void inativarItem(ItemMaterial item) { exigirItemDoEstoque(item); item.inativar(); }
    public void liberarItem(ItemMaterial item) { exigirItemDoEstoque(item); item.liberar(); }

    public synchronized void cadastrarRecebedor(Recebedor recebedor, byte[] imagem) {
        exigirAtivo();
        Objects.requireNonNull(recebedor, "O recebedor é obrigatório.");
        if (!recebedor.isAtivo()) throw new IllegalArgumentException("Não é possível cadastrar um recebedor inativo.");
        if (imagem == null || imagem.length == 0) throw new IllegalArgumentException("A imagem de cadastro facial é obrigatória.");
        if (recebedores.stream().anyMatch(atual -> atual.getMatricula().equalsIgnoreCase(recebedor.getMatricula()))) {
            throw new IllegalArgumentException("Já existe recebedor com essa matrícula.");
        }
        if (verificadorFacial == null) throw new IllegalStateException("Configure um provedor de reconhecimento facial antes do cadastro biométrico.");
        verificadorFacial.cadastrar(recebedor, imagem);
        if (!recebedor.possuiFaceCadastrada()) throw new IllegalStateException("O provedor não cadastrou o template facial do recebedor.");
        recebedores.add(recebedor);
    }

    public void registrarExtravio(Empenho empenho, ItemEmpenho item) {
        exigirAtivo();
        Objects.requireNonNull(empenho, "O empenho é obrigatório.");
        Objects.requireNonNull(item, "O item do empenho é obrigatório.");
        if (!estoque.contem(item.getItemMaterial())) throw new IllegalArgumentException("O item não pertence a este estoque.");
        empenho.marcarItemExtraviado(item, this);
    }

    private void exigirItemDoEstoque(ItemMaterial item) {
        exigirAtivo();
        if (!estoque.contem(item)) throw new IllegalArgumentException("O item não pertence a este estoque.");
    }

    private void exigirAtivo() {
        if (!isAtivo()) throw new IllegalStateException("A conta do administrador do estoque está inativa.");
    }

    @Override public String getPerfil() { return "ADMINISTRADOR_ESTOQUE"; }
    @Override public boolean podeGerenciarMaterial() { return true; }
    @Override public boolean podeRegistrarMovimentacao() { return false; }
}





