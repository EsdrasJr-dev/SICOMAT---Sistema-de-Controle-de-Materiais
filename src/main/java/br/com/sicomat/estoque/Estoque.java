package br.com.sicomat.estoque;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Estoque {

    private final String secao;
    private final List<Material> materiais = new ArrayList<>();

    public Estoque(String secao) {
        if (secao == null || secao.isBlank()) throw new IllegalArgumentException("A seção do estoque é obrigatória.");
        this.secao = secao.trim();
    }

    public String getSecao() { return secao; }

    public synchronized Material buscarMaterial(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        return materiais.stream().filter(Material::isAtivo)
                .filter(material -> material.getCodigo().equalsIgnoreCase(codigo.trim()))
                .findFirst().orElse(null);
    }

    public synchronized List<Material> listarMateriais() {
        return Collections.unmodifiableList(new ArrayList<>(materiais));
    }

    public synchronized List<Material> listarDisponiveis() {
        List<Material> disponiveis = new ArrayList<>();
        for (Material material : materiais) {
            if (material.isAtivo() && material.getQuantidadeDisponivel() > 0) disponiveis.add(material);
        }
        return Collections.unmodifiableList(disponiveis);
    }

    public synchronized List<ItemMaterial> listarItensDisponiveis() {
        List<ItemMaterial> itens = new ArrayList<>();
        for (Material material : materiais) {
            if (material.isAtivo()) itens.addAll(material.listarItensDisponiveis());
        }
        return Collections.unmodifiableList(itens);
    }

    public synchronized void adicionarMaterial(Material material) {
        Objects.requireNonNull(material, "O material é obrigatório.");
        if (!material.isAtivo()) throw new IllegalArgumentException("Não é possível cadastrar um material inativo.");
        if (materiais.stream().anyMatch(atual -> atual.getCodigo().equalsIgnoreCase(material.getCodigo()))) {
            throw new IllegalArgumentException("Já existe material com esse código.");
        }
        materiais.add(material);
    }

    public synchronized void editarMaterial(Material material, String novoCodigo, String novoNome) {
        Objects.requireNonNull(material, "O material é obrigatório.");
        if (!materiais.contains(material)) throw new IllegalArgumentException("O material não pertence a este estoque.");
        boolean codigoEmUso = materiais.stream().anyMatch(atual -> atual != material
                && atual.getCodigo().equalsIgnoreCase(novoCodigo == null ? "" : novoCodigo.trim()));
        if (codigoEmUso) throw new IllegalArgumentException("Já existe material com esse código.");
        material.editarCadastro(novoCodigo, novoNome);
    }

    public synchronized void adicionarItem(Material material, String patrimonio) {
        Objects.requireNonNull(material, "O material é obrigatório.");
        if (!materiais.contains(material)) throw new IllegalArgumentException("O material não pertence a este estoque.");
        boolean patrimonioEmUso = materiais.stream().flatMap(atual -> atual.getItens().stream())
                .anyMatch(item -> item.getNumeroPatrimonio().equalsIgnoreCase(patrimonio == null ? "" : patrimonio.trim()));
        if (patrimonioEmUso) throw new IllegalArgumentException("Já existe item com esse patrimônio neste estoque.");
        material.adicionarItem(new ItemMaterial(patrimonio));
    }

    public synchronized boolean contem(Material material) { return material != null && materiais.contains(material); }

    public synchronized boolean contem(ItemMaterial item) {
        return item != null && item.getMaterial() != null && materiais.contains(item.getMaterial())
                && item.getMaterial().getItens().contains(item);
    }
}

