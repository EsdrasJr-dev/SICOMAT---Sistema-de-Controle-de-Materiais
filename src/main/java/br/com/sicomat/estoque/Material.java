package br.com.sicomat.estoque;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Material {

    private String codigo;
    private String nome;
    private volatile boolean ativo = true;
    private final List<ItemMaterial> itens = new ArrayList<>();

    public Material(String codigo, String nome) {
        this.codigo = textoObrigatorio(codigo, "código");
        this.nome = textoObrigatorio(nome, "nome");
    }

    public synchronized String getCodigo() { return codigo; }
    public synchronized String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }

    public synchronized int getQuantidadeTotal() { return itens.size(); }

    public synchronized int getQuantidadeDisponivel() {
        return (int) itens.stream().filter(ItemMaterial::estarDisponivel).count();
    }

    public synchronized void adicionarItem(ItemMaterial item) {
        Objects.requireNonNull(item, "O item físico é obrigatório.");
        if (!ativo) throw new IllegalStateException("Não é possível incluir itens em um material inativo.");
        if (itens.stream().anyMatch(atual -> atual.getNumeroPatrimonio().equalsIgnoreCase(item.getNumeroPatrimonio()))) {
            throw new IllegalArgumentException("Este patrimônio já está cadastrado neste material.");
        }
        item.associarMaterial(this);
        itens.add(item);
    }

    public synchronized List<ItemMaterial> getItens() {
        return Collections.unmodifiableList(new ArrayList<>(itens));
    }

    public synchronized List<ItemMaterial> listarItensDisponiveis() {
        List<ItemMaterial> disponiveis = new ArrayList<>();
        for (ItemMaterial item : itens) if (item.estarDisponivel()) disponiveis.add(item);
        return Collections.unmodifiableList(disponiveis);
    }

    public synchronized void editarCadastro(String novoCodigo, String novoNome) {
        if (!ativo) throw new IllegalStateException("Não é possível editar um material inativo.");
        codigo = textoObrigatorio(novoCodigo, "código");
        nome = textoObrigatorio(novoNome, "nome");
    }

    public synchronized void inativar() {
        if (!ativo) return;
        ativo = false;
        for (ItemMaterial item : itens) {
            if (!item.isReservado() && (item.getStatus() == StatusItem.DISPONIVEL || item.getStatus() == StatusItem.AVARIADO)) item.inativar();
        }
    }

    private static String textoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("O campo " + campo + " é obrigatório.");
        return valor.trim();
    }
}


