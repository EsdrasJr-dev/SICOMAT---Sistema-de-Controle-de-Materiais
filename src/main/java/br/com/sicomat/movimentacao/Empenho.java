package br.com.sicomat.movimentacao;

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
        if (itens == null || itens.isEmpty()) throw new IllegalArgumentException("O empenho precisa conter ao menos um item.");

        Set<ItemMaterial> unicos = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ItemMaterial> reservados = new ArrayList<>();
        try {
            for (ItemMaterial item : itens) {
                Objects.requireNonNull(item, "O item físico não pode ser nulo.");
                if (!unicos.add(item)) throw new IllegalArgumentException("O mesmo item não pode aparecer duas vezes no empenho.");
                ItemEmpenho itemEmpenho = new ItemEmpenho(this, item);
                item.reservar(this);
                reservados.add(item);
                this.itens.add(itemEmpenho);
            }
            registrar();
        } catch (RuntimeException ex) {
            for (ItemMaterial item : reservados) item.liberarReserva(this);
            throw ex;
        }
    }

    public synchronized StatusEmpenho getStatus() { recalcularStatus(); return status; }
    public synchronized LocalDateTime getConfirmadoEm() { return confirmadoEm; }
    public synchronized boolean isConfirmadoPorFace() { return confirmadoPorFace; }
    public synchronized double getScoreFacial() { return scoreFacial; }
    public synchronized String getJustificativaManual() { return justificativaManual; }
    public Recebedor getRecebedor() { return recebedor; }
    public Equipe getEquipe() { return equipe; }
    public synchronized List<ItemEmpenho> getItens() { return Collections.unmodifiableList(new ArrayList<>(itens)); }
    public synchronized List<Devolucao> getDevolucoes() { return Collections.unmodifiableList(new ArrayList<>(devolucoes)); }

    @Override public synchronized void registrar() {
        if (status == StatusEmpenho.CANCELADO) throw new IllegalStateException("Um empenho cancelado não pode ser registrado novamente.");
        registrado = true;
    }

    /** Confirma após validar a imagem com o provedor biométrico configurado. */
    public synchronized boolean confirmarComFace(VerificadorFacial verificador, byte[] imagem) {
        exigirPendente();
        if (verificador == null || imagem == null || imagem.length == 0 || !recebedor.possuiFaceCadastrada()) return false;
        double score = verificador.calcularScore(recebedor, imagem);
        if (!Double.isFinite(score) || !verificador.aprovado(score)) return false;
        confirmarItens();
        confirmadoPorFace = true;
        scoreFacial = score;
        confirmadoEm = LocalDateTime.now();
        status = StatusEmpenho.ABERTO;
        return true;
    }

    /** Confirma após conferir o PIN do recebedor e registrar a justificativa manual. */
    public synchronized boolean confirmarComPin(String pin, String justificativa) {
        exigirPendente();
        if (justificativa == null || justificativa.isBlank()) throw new IllegalArgumentException("A justificativa é obrigatória.");
        if (!recebedor.verificarPin(pin)) return false;
        confirmarItens();
        confirmadoPorFace = false;
        scoreFacial = 0.0;
        justificativaManual = justificativa.trim();
        confirmadoEm = LocalDateTime.now();
        status = StatusEmpenho.ABERTO;
        return true;
    }

    /** Transição interna para fluxos que já autenticaram o recebedor. */
    synchronized void confirmar() {
        exigirPendente();
        confirmarItens();
        confirmadoPorFace = false;
        confirmadoEm = LocalDateTime.now();
        status = StatusEmpenho.ABERTO;
    }

    private void confirmarItens() {
        if (!registrado) throw new IllegalStateException("O empenho ainda não foi registrado.");
        for (ItemEmpenho item : itens) item.validarConfirmacao();
        for (ItemEmpenho item : itens) item.confirmar();
    }

    public synchronized void cancelar() {
        exigirPendente();
        for (ItemEmpenho item : itens) item.getItemMaterial().liberarReserva(this);
        status = StatusEmpenho.CANCELADO;
    }

    public synchronized List<ItemEmpenho> getItensPendentes() {
        recalcularStatus();
        List<ItemEmpenho> pendentes = new ArrayList<>();
        if (status != StatusEmpenho.PENDENTE && status != StatusEmpenho.CANCELADO) {
            for (ItemEmpenho item : itens) if (item.estaPendente()) pendentes.add(item);
        }
        return Collections.unmodifiableList(pendentes);
    }

    public synchronized boolean emAberto() {
        recalcularStatus();
        return status == StatusEmpenho.ABERTO || status == StatusEmpenho.DEVOLVIDO_PARCIALMENTE;
    }

    public synchronized void marcarItemExtraviado(ItemEmpenho item, AdministradorEstoque registradoPor) {
        if (status == StatusEmpenho.PENDENTE || status == StatusEmpenho.CANCELADO) {
            throw new IllegalStateException("Só é possível registrar extravio de item de empenho confirmado.");
        }
        if (item == null || !itens.contains(item)) throw new IllegalArgumentException("O item não pertence a este empenho.");
        item.marcarExtraviado(registradoPor);
        recalcularStatus();
    }

    synchronized void adicionarDevolucao(Devolucao devolucao) {
        if (!devolucoes.contains(devolucao)) devolucoes.add(devolucao);
        recalcularStatus();
    }

    private void exigirPendente() {
        if (!registrado || status != StatusEmpenho.PENDENTE) {
            throw new IllegalStateException("A operação só é permitida enquanto o empenho está pendente.");
        }
        if (!recebedor.isAtivo()) throw new IllegalStateException("O recebedor está inativo.");
    }

    private void recalcularStatus() {
        if (status == StatusEmpenho.PENDENTE || status == StatusEmpenho.CANCELADO) return;
        boolean temPendente = itens.stream().anyMatch(ItemEmpenho::estaPendente);
        boolean temDevolvido = itens.stream().anyMatch(item -> item.getSituacao() == SituacaoItemEmpenho.DEVOLVIDO);
        boolean temExtraviado = itens.stream().anyMatch(item -> item.getSituacao() == SituacaoItemEmpenho.EXTRAVIADO);
        if (temPendente) status = temDevolvido || temExtraviado
                ? StatusEmpenho.DEVOLVIDO_PARCIALMENTE : StatusEmpenho.ABERTO;
        else status = temExtraviado ? StatusEmpenho.EXTRAVIADO : StatusEmpenho.ENCERRADO;
    }
}



