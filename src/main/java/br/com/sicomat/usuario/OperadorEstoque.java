package br.com.sicomat.usuario;

import br.com.sicomat.estoque.Estoque;
import br.com.sicomat.estoque.ItemMaterial;
import br.com.sicomat.equipe.Equipe;
import br.com.sicomat.equipe.Recebedor;
import br.com.sicomat.movimentacao.Devolucao;
import br.com.sicomat.movimentacao.Empenho;
import br.com.sicomat.movimentacao.ItemDevolucao;
import br.com.sicomat.painel.PainelConferencia;
import br.com.sicomat.seguranca.VerificadorFacial;
import java.util.List;
import java.util.Objects;

public class OperadorEstoque extends Usuario {

    private final String turno;
    private final Estoque estoque;
    private final VerificadorFacial verificadorFacial;
    private final PainelConferencia painel;

    public OperadorEstoque(String nome, String matricula, String senhaHash, String turno,
                           Estoque estoque, VerificadorFacial verificadorFacial) {
        this(nome, matricula, senhaHash, turno, estoque, verificadorFacial, null);
    }

    public OperadorEstoque(String nome, String matricula, String senhaHash, String turno,
                           Estoque estoque, VerificadorFacial verificadorFacial, PainelConferencia painel) {
        super(nome, matricula, senhaHash);
        this.turno = textoObrigatorio(turno, "turno");
        this.estoque = Objects.requireNonNull(estoque, "O estoque é obrigatório para o operador.");
        this.verificadorFacial = verificadorFacial;
        this.painel = painel;
    }

    public String getTurno() { return turno; }

    public Empenho iniciarEmpenho(Recebedor recebedor, Equipe equipe, List<ItemMaterial> itens) {
        exigirAtivo();
        validarItensParaEmpenho(itens);
        Empenho empenho = new Empenho(this, recebedor, equipe, itens);
        if (painel != null) painel.registrar(empenho);
        return empenho;
    }

    /** Garante que todos os itens do empenho pertençam ao estoque deste operador. */
    public void validarItensParaEmpenho(List<ItemMaterial> itens) {
        exigirAtivo();
        if (itens == null || itens.isEmpty()) throw new IllegalArgumentException("Informe ao menos um item do estoque.");
        for (ItemMaterial item : itens) {
            if (!estoque.contem(item)) throw new IllegalArgumentException("Todos os itens devem pertencer ao estoque do operador.");
        }
    }

    /** Permite que outro turno continue o fluxo, desde que opere o mesmo estoque. */
    public void validarAcesso(Empenho empenho) {
        exigirAtivo();
        if (empenho == null || !empenho.pertenceAoEstoque(estoque)) {
            throw new IllegalArgumentException("O empenho não pertence ao estoque deste operador.");
        }
    }

    public boolean confirmarEmpenho(Empenho empenho, byte[] imagem) {
        validarAcesso(empenho);
        return empenho.confirmarComFace(this, verificadorFacial, imagem);
    }

    public boolean confirmarComPin(Empenho empenho, String pin, String justificativa) {
        validarAcesso(empenho);
        return empenho.confirmarComPin(this, pin, justificativa);
    }

    public void cancelarEmpenho(Empenho empenho) {
        validarAcesso(empenho);
        empenho.cancelar(this);
    }

    public Devolucao registrarDevolucao(List<ItemDevolucao> itens) {
        return registrarDevolucao(null, itens, "");
    }

    /** Registra o operador da vez e a pessoa que efetivamente entregou os itens. */
    public Devolucao registrarDevolucao(Recebedor devolvidoPor, List<ItemDevolucao> itens, String observacao) {
        exigirAtivo();
        if (itens == null || itens.isEmpty()) throw new IllegalArgumentException("Informe ao menos um item para devolver.");
        ItemDevolucao primeiro = Objects.requireNonNull(itens.get(0), "Item de devolução inválido.");
        Empenho empenho = primeiro.getItemEmpenho().getEmpenho();
        validarAcesso(empenho);
        Recebedor pessoaQueDevolveu = devolvidoPor == null ? empenho.getRecebedor() : devolvidoPor;
        Devolucao devolucao = new Devolucao(empenho, pessoaQueDevolveu, this, itens, observacao);
        devolucao.registrar();
        return devolucao;
    }

    public List<ItemMaterial> consultarItensDisponiveis() {
        exigirAtivo();
        return estoque.listarItensDisponiveis();
    }

    private void exigirAtivo() {
        if (!isAtivo()) throw new IllegalStateException("A conta do operador está inativa.");
    }

    @Override public String getPerfil() { return "OPERADOR_ESTOQUE"; }
    @Override public boolean podeGerenciarMaterial() { return false; }
    @Override public boolean podeRegistrarMovimentacao() { return true; }
}
