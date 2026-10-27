package br.com.sicomat.movimentacao;

import br.com.sicomat.usuario.OperadorEstoque;
import java.time.LocalDateTime;

public abstract class Movimentacao {

    private final LocalDateTime dataHora;
    private final String observacao;
    private final OperadorEstoque registradoPor;

    protected Movimentacao(OperadorEstoque registradoPor, String observacao) {
        this(registradoPor, observacao, LocalDateTime.now());
    }

    protected Movimentacao(OperadorEstoque registradoPor, String observacao, LocalDateTime dataHora) {
        if (registradoPor == null || !registradoPor.isAtivo()) {
            throw new IllegalArgumentException("A movimentação precisa ser registrada por um operador ativo.");
        }
        if (dataHora == null) throw new IllegalArgumentException("A data e a hora são obrigatórias.");
        this.registradoPor = registradoPor;
        this.observacao = observacao == null ? "" : observacao.trim();
        this.dataHora = dataHora;
    }

    public LocalDateTime getDataHora() { return dataHora; }
    public String getObservacao() { return observacao; }
    public OperadorEstoque getRegistradoPor() { return registradoPor; }
    public abstract void registrar();
}
