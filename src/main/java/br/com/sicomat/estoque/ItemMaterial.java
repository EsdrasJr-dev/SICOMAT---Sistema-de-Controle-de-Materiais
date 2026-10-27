package br.com.sicomat.estoque;

import br.com.sicomat.movimentacao.Empenho;
import java.util.Objects;

public class ItemMaterial {

    private final String numeroPatrimonio;
    private StatusItem status = StatusItem.DISPONIVEL;
    private Material material;
    private Empenho reservadoPor;

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
    public synchronized boolean reservaPertenceA(Empenho empenho) { return reservadoPor == empenho; }

    public synchronized boolean estarDisponivel() {
        return status == StatusItem.DISPONIVEL && reservadoPor == null && (material == null || material.isAtivo());
    }

    public synchronized void empenhar() {
        if (!estarDisponivel()) throw new IllegalStateException("O item não está disponível para empenho.");
        status = StatusItem.EMPENHADO;
    }

    public synchronized void reservar(Empenho empenho) {
        Objects.requireNonNull(empenho, "O empenho é obrigatório.");
        if (!estarDisponivel()) throw new IllegalStateException("O item não está disponível para reserva.");
        reservadoPor = empenho;
    }

    public synchronized void confirmarEmpenho(Empenho empenho) {
        if (reservadoPor != empenho) throw new IllegalStateException("O item não está reservado por este empenho.");
        if (material != null && !material.isAtivo()) throw new IllegalStateException("O material foi inativado antes da confirmação.");
        status = StatusItem.EMPENHADO;
        reservadoPor = null;
    }

    public synchronized void liberarReserva(Empenho empenho) {
        if (reservadoPor == empenho) reservadoPor = null;
    }

    public synchronized void devolver() { devolver(false); }

    public synchronized void devolver(boolean avariado) {
        if (status != StatusItem.EMPENHADO) throw new IllegalStateException("Só é possível devolver um item empenhado.");
        status = avariado ? StatusItem.AVARIADO
                : (material != null && !material.isAtivo() ? StatusItem.INATIVO : StatusItem.DISPONIVEL);
    }

    public synchronized void marcarExtraviado() {
        if (status != StatusItem.EMPENHADO) throw new IllegalStateException("Só é possível registrar extravio de item empenhado.");
        status = StatusItem.EXTRAVIADO;
    }

    public synchronized void marcarAvariado() {
        if (reservadoPor != null) throw new IllegalStateException("Um item reservado não pode ser marcado como avariado.");
        if (status != StatusItem.EMPENHADO && status != StatusItem.DISPONIVEL) {
            throw new IllegalStateException("O item não pode ser marcado como avariado neste estado.");
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




