package br.com.sicomat.movimentacao;

import br.com.sicomat.estoque.Estoque;
import br.com.sicomat.estoque.ItemMaterial;
import br.com.sicomat.equipe.Equipe;
import br.com.sicomat.equipe.Recebedor;
import br.com.sicomat.seguranca.VerificadorFacial;
import br.com.sicomat.usuario.AdministradorEstoque;
import br.com.sicomat.usuario.OperadorEstoque;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Empenho extends Movimentacao {

    private StatusEmpenho status = StatusEmpenho.PENDENTE;
    private LocalDateTime confirmadoEm;
    private OperadorEstoque confirmadoPor;
    private OperadorEstoque canceladoPor;
    private boolean confirmadoPorFace;
    private double scoreFacial;
    private String justificativaManual;
    private final Recebedor recebedor;
    private final Equipe equipe;
    private final List<ItemEmpenho> itens = new ArrayList<>();
    private final List<Devolucao> devolucoes = new ArrayList<>();
    private boolean registrado;

    public Empenho(OperadorEstoque operador, Recebedor recebedor, Equipe equipe, List<ItemMaterial> itens) {
        super(operador, "");
        this.recebedor = Objects.requireNonNull(recebedor, "O recebedor é obrigatório.");
        this.equipe = Objects.requireNonNull(equipe, "A equipe é obrigatória.");
        if (!recebedor.isAtivo()) throw new IllegalArgumentException("O recebedor está inativo.");
        if (!recebedor.possuiFaceCadastrada() || recebedor.getConsentimentoEm() == null) {
            throw new IllegalArgumentException("O recebedor precisa ter cadastro facial válido e consentido antes do empenho.");
        }
        if (itens == null || itens.isEmpty()) throw new IllegalArgumentException("O empenho precisa conter ao menos um item.");
        operador.validarItensParaEmpenho(itens);

        Set<ItemMaterial> unicos = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ItemEmpenho> reservados = new ArrayList<>();
        try {
            for (ItemMaterial item : itens) {
                Objects.requireNonNull(item, "O item físico não pode ser nulo.");
                if (!unicos.add(item)) throw new IllegalArgumentException("O mesmo item não pode aparecer duas vezes no empenho.");
                ItemEmpenho itemEmpenho = new ItemEmpenho(this, item);
                itemEmpenho.reservar();
                reservados.add(itemEmpenho);
                this.itens.add(itemEmpenho);
            }
            registrar();
        } catch (RuntimeException ex) {
            for (ItemEmpenho item : reservados) item.liberarReserva();
            throw ex;
        }
    }

    public synchronized StatusEmpenho getStatus() { return status; }
    public synchronized LocalDateTime getConfirmadoEm() { return confirmadoEm; }
    public synchronized OperadorEstoque getConfirmadoPor() { return confirmadoPor; }
    public synchronized OperadorEstoque getCanceladoPor() { return canceladoPor; }
    public synchronized boolean isConfirmadoPorFace() { return confirmadoPorFace; }
    public synchronized double getScoreFacial() { return scoreFacial; }
    public synchronized String getJustificativaManual() { return justificativaManual; }
    public Recebedor getRecebedor() { return recebedor; }
    public Equipe getEquipe() { return equipe; }
    public synchronized List<ItemEmpenho> getItens() { return Collections.unmodifiableList(new ArrayList<>(itens)); }
    public synchronized List<Devolucao> getDevolucoes() { return Collections.unmodifiableList(new ArrayList<>(devolucoes)); }

    public synchronized boolean pertenceAoEstoque(Estoque estoque) {
        return estoque != null && !itens.isEmpty()
                && itens.stream().allMatch(item -> estoque.contem(item.getItemMaterial()));
    }

    @Override public synchronized void registrar() {
        if (status == StatusEmpenho.CANCELADO) throw new IllegalStateException("Um empenho cancelado não pode ser registrado novamente.");
        registrado = true;
    }

    /** Confirma após validar a imagem com o provedor biométrico configurado. */
    public synchronized boolean confirmarComFace(OperadorEstoque operador, VerificadorFacial verificador, byte[] imagem) {
        validarOperador(operador);
        exigirPendente();
        exigirRecebedorAtivo();
        if (verificador == null) throw new IllegalStateException("O provedor de reconhecimento facial não está configurado.");
        if (imagem == null || imagem.length == 0) throw new IllegalArgumentException("A imagem para confirmação facial é obrigatória.");
        if (!recebedor.possuiFaceCadastrada() || recebedor.getConsentimentoEm() == null) {
            throw new IllegalStateException("O recebedor não possui cadastro facial válido e consentido.");
        }
        double score = verificador.calcularScore(recebedor, imagem);
        if (!Double.isFinite(score) || !verificador.aprovado(score)) return false;
        confirmarItens();
        confirmadoPorFace = true;
        scoreFacial = score;
        justificativaManual = null;
        confirmadoEm = LocalDateTime.now();
        confirmadoPor = operador;
        status = StatusEmpenho.ABERTO;
        return true;
    }

    /** PIN é a exceção quando a conferência facial não pode ser concluída e exige justificativa. */
    public synchronized boolean confirmarComPin(OperadorEstoque operador, String pin, String justificativa) {
        validarOperador(operador);
        exigirPendente();
        exigirRecebedorAtivo();
        if (justificativa == null || justificativa.isBlank()) throw new IllegalArgumentException("A justificativa é obrigatória.");
        if (!recebedor.verificarPin(pin)) return false;
        confirmarItens();
        confirmadoPorFace = false;
        scoreFacial = 0.0;
        justificativaManual = justificativa.trim();
        confirmadoEm = LocalDateTime.now();
        confirmadoPor = operador;
        status = StatusEmpenho.ABERTO;
        return true;
    }

    public synchronized void cancelar(OperadorEstoque operador) {
        validarOperador(operador);
        exigirPendente();
        for (ItemEmpenho item : itens) item.liberarReserva();
        status = StatusEmpenho.CANCELADO;
        canceladoPor = operador;
    }

    public synchronized List<ItemEmpenho> getItensPendentes() {
        List<ItemEmpenho> pendentes = new ArrayList<>();
        if (status == StatusEmpenho.ABERTO || status == StatusEmpenho.DEVOLVIDO_PARCIALMENTE) {
            for (ItemEmpenho item : itens) if (item.estaPendente()) pendentes.add(item);
        }
        return Collections.unmodifiableList(pendentes);
    }

    public synchronized boolean emAberto() {
        return status == StatusEmpenho.ABERTO || status == StatusEmpenho.DEVOLVIDO_PARCIALMENTE;
    }

    public synchronized void marcarItemExtraviado(ItemEmpenho item, AdministradorEstoque registradoPor) {
        if (status == StatusEmpenho.PENDENTE || status == StatusEmpenho.CANCELADO) {
            throw new IllegalStateException("Só é possível registrar extravio de item de empenho confirmado.");
        }
        if (item == null || !itens.contains(item)) throw new IllegalArgumentException("O item não pertence a este empenho.");
        item.marcarExtraviado(registradoPor);
        atualizarStatus();
    }

    synchronized void adicionarDevolucao(Devolucao devolucao) {
        if (!devolucoes.contains(devolucao)) devolucoes.add(devolucao);
        atualizarStatus();
    }

    private void confirmarItens() {
        if (!registrado) throw new IllegalStateException("O empenho ainda não foi registrado.");
        for (ItemEmpenho item : itens) item.validarConfirmacao();

        List<ItemEmpenho> confirmados = new ArrayList<>();
        try {
            for (ItemEmpenho item : itens) {
                item.confirmar();
                confirmados.add(item);
            }
        } catch (RuntimeException ex) {
            for (int i = confirmados.size() - 1; i >= 0; i--) {
                try {
                    confirmados.get(i).reverterConfirmacao();
                } catch (RuntimeException erroReversao) {
                    ex.addSuppressed(erroReversao);
                }
            }
            throw ex;
        }
    }

    private void validarOperador(OperadorEstoque operador) {
        Objects.requireNonNull(operador, "O operador atual é obrigatório.").validarAcesso(this);
    }

    private void exigirPendente() {
        if (!registrado || status != StatusEmpenho.PENDENTE) {
            throw new IllegalStateException("A operação só é permitida enquanto o empenho está pendente.");
        }
    }

    private void exigirRecebedorAtivo() {
        if (!recebedor.isAtivo()) throw new IllegalStateException("O recebedor está inativo.");
    }

    private void atualizarStatus() {
        if (status == StatusEmpenho.PENDENTE || status == StatusEmpenho.CANCELADO) return;
        boolean temPendente = itens.stream().anyMatch(ItemEmpenho::estaPendente);
        boolean temDevolvido = itens.stream().anyMatch(item -> item.getSituacao() == SituacaoItemEmpenho.DEVOLVIDO);
        boolean temExtraviado = itens.stream().anyMatch(item -> item.getSituacao() == SituacaoItemEmpenho.EXTRAVIADO);
        if (temPendente) {
            status = temDevolvido || temExtraviado
                    ? StatusEmpenho.DEVOLVIDO_PARCIALMENTE : StatusEmpenho.ABERTO;
        } else {
            status = temExtraviado ? StatusEmpenho.EXTRAVIADO : StatusEmpenho.ENCERRADO;
        }
    }
}
