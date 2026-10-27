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

    public OperadorEstoque(String nome, String matricula, String senhaHash, String turno) {
        this(nome, matricula, senhaHash, turno, null, null, null);
    }

    public OperadorEstoque(String nome, String matricula, String senhaHash, String turno,
                           Estoque estoque, VerificadorFacial verificadorFacial) {
        this(nome, matricula, senhaHash, turno, estoque, verificadorFacial, null);
    }

    public OperadorEstoque(String nome, String matricula, String senhaHash, String turno,
                           Estoque estoque, VerificadorFacial verificadorFacial, PainelConferencia painel) {
        super(nome, matricula, senhaHash);
        this.turno = textoObrigatorio(turno, "turno");
        this.estoque = estoque;
        this.verificadorFacial = verificadorFacial;
        this.painel = painel;
    }

    public String getTurno() { return turno; }

    public Empenho iniciarEmpenho(Recebedor recebedor, Equipe equipe, List<ItemMaterial> itens) {
        exigirAtivo();
        Empenho empenho = new Empenho(this, recebedor, equipe, itens);
        if (painel != null) painel.registrar(empenho);
        return empenho;
    }

    public boolean confirmarEmpenho(Empenho empenho, byte[] imagem) {
        exigirEmpenhoDoOperador(empenho);
        return empenho.confirmarComFace(verificadorFacial, imagem);
    }

    public boolean confirmarComPin(Empenho empenho, String pin, String justificativa) {
        exigirEmpenhoDoOperador(empenho);
        return empenho.confirmarComPin(pin, justificativa);
    }

    public void cancelarEmpenho(Empenho empenho) { exigirEmpenhoDoOperador(empenho); empenho.cancelar(); }

    public Devolucao registrarDevolucao(List<ItemDevolucao> itens) {
        exigirAtivo();
        if (itens == null || itens.isEmpty()) throw new IllegalArgumentException("Informe ao menos um item para devolver.");
        ItemDevolucao primeiro = Objects.requireNonNull(itens.get(0), "Item de devolução inválido.");
        Empenho empenho = primeiro.getItemEmpenho().getEmpenho();
        if (empenho.getRegistradoPor() != this) throw new IllegalArgumentException("O empenho pertence a outro operador.");
        Devolucao devolucao = new Devolucao(empenho, empenho.getRecebedor(), this, itens, "");
        devolucao.registrar();
        return devolucao;
    }

    public List<ItemMaterial> consultarItensDisponiveis() {
        exigirAtivo();
        if (estoque == null) throw new IllegalStateException("Configure o estoque no operador antes de consultar os itens.");
        return estoque.listarItensDisponiveis();
    }

    private void exigirEmpenhoDoOperador(Empenho empenho) {
        exigirAtivo();
        if (empenho == null || empenho.getRegistradoPor() != this) {
            throw new IllegalArgumentException("O empenho não foi registrado por este operador.");
        }
    }

    private void exigirAtivo() {
        if (!isAtivo()) throw new IllegalStateException("A conta do operador está inativa.");
    }

    @Override public String getPerfil() { return "OPERADOR_ESTOQUE"; }
    @Override public boolean podeGerenciarMaterial() { return false; }
    @Override public boolean podeRegistrarMovimentacao() { return true; }
}

