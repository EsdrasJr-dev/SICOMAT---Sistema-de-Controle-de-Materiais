package br.com.sicomat.estoque;

import br.com.sicomat.movimentacao.Empenho;
import java.util.Objects;

public class ItemMaterial {

    private final String numeroPatrimonio;
    private StatusItem status = StatusItem.DISPONIVEL;
    private Material material;
    private Empenho reservadoPor;
    private Object tokenReserva;
    private Object tokenMovimentacao;

    public ItemMaterial(String numeroPatrimonio) {
        if (numeroPatrimonio == null || numeroPatrimonio.isBlank()) throw new IllegalArgumentException("O patrimônio é obrigatório.");
        this.numeroPatrimonio = numeroPatrimonio.trim();
    }

    public ItemMaterial(String numeroPatrimonio, Material material) {
        this(numeroPatrimonio);
        Objects.requireNonNull(material, "O material é obrigatório.").adicionarItem(this);
    }

    public String getNumeroPatrimonio() { return numeroPatrimonio; }
    public synchronized StatusItem getStatus() { return status; }
    public synchronized Material getMaterial() { return material; }
    public synchronized boolean estaEmpenhado() { return status == StatusItem.EMPENHADO; }
    public synchronized boolean isReservado() { return reservadoPor != null; }
    public synchronized boolean reservaPertenceA(Empenho empenho, Object token) {
        return reservadoPor == empenho && tokenReserva == token && token != null;
    }

    public synchronized boolean estarDisponivel() {
        return status == StatusItem.DISPONIVEL && reservadoPor == null && (material == null || material.isAtivo());
    }

    /** O token é mantido privadamente por ItemEmpenho e impede alterações fora do fluxo do empenho. */
    public synchronized void reservar(Empenho empenho, Object token) {
        Objects.requireNonNull(empenho, "O empenho é obrigatório.");
        Objects.requireNonNull(token, "A autorização da linha do empenho é obrigatória.");
        if (!estarDisponivel()) throw new IllegalStateException("O item não está disponível para reserva.");
        reservadoPor = empenho;
        tokenReserva = token;
    }

    public synchronized void confirmarEmpenho(Empenho empenho, Object token) {
        if (reservadoPor != empenho || tokenReserva != token || token == null || status != StatusItem.DISPONIVEL) {
            throw new IllegalStateException("O item não está reservado por este empenho.");
        }
        if (material == null || !material.isAtivo()) {
            throw new IllegalStateException("O material foi inativado antes da confirmação.");
        }
        status = StatusItem.EMPENHADO;
        reservadoPor = null;
        tokenReserva = null;
        tokenMovimentacao = token;
    }

    /** Restaura uma reserva se uma confirmação com vários itens falhar no meio do processo. */
    public synchronized void reverterConfirmacao(Empenho empenho, Object token) {
        if (status != StatusItem.EMPENHADO || tokenMovimentacao != token || token == null) {
            throw new IllegalStateException("Não é possível reverter o estado atual do item.");
        }
        status = StatusItem.DISPONIVEL;
        reservadoPor = Objects.requireNonNull(empenho, "O empenho é obrigatório.");
        tokenReserva = token;
        tokenMovimentacao = null;
    }

    public synchronized void liberarReserva(Empenho empenho, Object token) {
        if (reservadoPor == empenho && tokenReserva == token && token != null) {
            reservadoPor = null;
            tokenReserva = null;
            if (material != null && !material.isAtivo() && status == StatusItem.DISPONIVEL) {
                status = StatusItem.INATIVO;
            }
        }
    }

    public synchronized void devolver(boolean avariado, Object token) {
        if (status != StatusItem.EMPENHADO || tokenMovimentacao != token || token == null) {
            throw new IllegalStateException("Só é possível devolver o item pelo empenho que o mantém registrado.");
        }
        status = avariado ? StatusItem.AVARIADO
                : (material != null && !material.isAtivo() ? StatusItem.INATIVO : StatusItem.DISPONIVEL);
        tokenMovimentacao = null;
    }

    public synchronized void marcarExtraviado(Object token) {
        if (status != StatusItem.EMPENHADO || tokenMovimentacao != token || token == null) {
            throw new IllegalStateException("Só é possível registrar extravio pelo empenho que mantém o item registrado.");
        }
        status = StatusItem.EXTRAVIADO;
        tokenMovimentacao = null;
    }

    public synchronized void marcarAvariado() {
        if (reservadoPor != null || status != StatusItem.DISPONIVEL) {
            throw new IllegalStateException("Só um item disponível pode ser marcado como avariado.");
        }
        status = StatusItem.AVARIADO;
    }

    public synchronized void liberar() {
        if (status != StatusItem.AVARIADO) throw new IllegalStateException("Só é possível liberar um item avariado.");
        status = material != null && !material.isAtivo() ? StatusItem.INATIVO : StatusItem.DISPONIVEL;
    }

    public synchronized void inativar() {
        if (reservadoPor != null || status == StatusItem.EMPENHADO) {
            throw new IllegalStateException("Um item reservado ou empenhado não pode ser inativado.");
        }
        if (status == StatusItem.EXTRAVIADO) throw new IllegalStateException("Um item extraviado deve permanecer no histórico.");
        status = StatusItem.INATIVO;
    }

    synchronized void associarMaterial(Material material) {
        if (this.material != null && this.material != material) throw new IllegalStateException("O item já pertence a outro material.");
        if (this.material == material) return;
        if (material.getItens().stream().anyMatch(item -> item != this
                && item.getNumeroPatrimonio().equalsIgnoreCase(numeroPatrimonio))) {
            throw new IllegalArgumentException("Já existe item com este patrimônio no material.");
        }
        this.material = material;
    }
}
