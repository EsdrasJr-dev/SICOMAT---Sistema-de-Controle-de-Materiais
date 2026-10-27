package br.com.sicomat.painel;

import br.com.sicomat.equipe.Equipe;
import br.com.sicomat.equipe.Recebedor;
import br.com.sicomat.movimentacao.Empenho;
import br.com.sicomat.movimentacao.ItemEmpenho;
import br.com.sicomat.movimentacao.SituacaoItemEmpenho;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Consulta em memória; a aplicação web poderá substituir a lista por consultas ao repositório. */
public class PainelConferencia {

    private final List<Empenho> empenhos = new ArrayList<>();

    public PainelConferencia() { }

    public PainelConferencia(Collection<Empenho> empenhos) {
        if (empenhos != null) for (Empenho empenho : empenhos) registrar(empenho);
    }

    public synchronized void registrar(Empenho empenho) {
        Objects.requireNonNull(empenho, "O empenho é obrigatório.");
        if (!empenhos.contains(empenho)) empenhos.add(empenho);
    }

    public synchronized List<Empenho> listarEmAberto() {
        return filtrar(Empenho::emAberto);
    }

    public synchronized List<Empenho> listarPorEquipe(Equipe equipe) {
        Objects.requireNonNull(equipe, "A equipe é obrigatória.");
        return filtrar(empenho -> empenho.getEquipe() == equipe);
    }

    public synchronized List<Empenho> listarPorRecebedor(Recebedor recebedor) {
        Objects.requireNonNull(recebedor, "O recebedor é obrigatório.");
        return filtrar(empenho -> empenho.getRecebedor() == recebedor);
    }

    public synchronized List<ItemEmpenho> listarExtraviados() {
        List<ItemEmpenho> extraviados = new ArrayList<>();
        for (Empenho empenho : empenhos) {
            for (ItemEmpenho item : empenho.getItens()) {
                if (item.getSituacao() == SituacaoItemEmpenho.EXTRAVIADO) extraviados.add(item);
            }
        }
        return Collections.unmodifiableList(extraviados);
    }

    private List<Empenho> filtrar(java.util.function.Predicate<Empenho> criterio) {
        List<Empenho> resultado = new ArrayList<>();
        for (Empenho empenho : empenhos) if (criterio.test(empenho)) resultado.add(empenho);
        return Collections.unmodifiableList(resultado);
    }
}


