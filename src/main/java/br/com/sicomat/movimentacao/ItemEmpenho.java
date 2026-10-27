package br.com.sicomat.movimentacao;

import br.com.sicomat.estoque.ItemMaterial;
import br.com.sicomat.usuario.AdministradorEstoque;
import java.time.LocalDateTime;
import java.util.Objects;

public class ItemEmpenho {

    private final Empenho empenho;
    private final ItemMaterial itemMaterial;
    private SituacaoItemEmpenho situacao;
    private LocalDateTime extraviadoEm;
    private AdministradorEstoque extravioRegistradoPor;

    ItemEmpenho(Empenho empenho, ItemMaterial itemMaterial) {
        this.empenho = Objects.requireNonNull(empenho, "O empenho é obrigatório.");
        this.itemMaterial = Objects.requireNonNull(itemMaterial, "O item físico é obrigatório.");
    }

    public Empenho getEmpenho() { return empenho; }
    public ItemMaterial getItemMaterial() { return itemMaterial; }
    public synchronized SituacaoItemEmpenho getSituacao() { return situacao; }
    public synchronized LocalDateTime getExtraviadoEm() { return extraviadoEm; }
    public synchronized AdministradorEstoque getExtravioRegistradoPor() { return extravioRegistradoPor; }
    public synchronized boolean estaPendente() { return situacao == SituacaoItemEmpenho.EMPENHADO; }

    synchronized void validarConfirmacao() {
        if (situacao != null || !itemMaterial.reservaPertenceA(empenho)) {
            throw new IllegalStateException("O item não está reservado para este empenho pendente.");
        }
    }

    synchronized void confirmar() {
        validarConfirmacao();
        itemMaterial.confirmarEmpenho(empenho);
        situacao = SituacaoItemEmpenho.EMPENHADO;
    }

    public synchronized void marcarDevolvido() { marcarDevolvido(false); }

    synchronized void marcarDevolvido(boolean avariado) {
        exigirEmpenhado();
        itemMaterial.devolver(avariado);
        situacao = SituacaoItemEmpenho.DEVOLVIDO;
    }

    synchronized void marcarExtraviado(AdministradorEstoque registradoPor) {
        exigirEmpenhado();
        if (registradoPor == null || !registradoPor.isAtivo()) {
            throw new IllegalArgumentException("O extravio deve ser registrado por um administrador do estoque ativo.");
        }
        itemMaterial.marcarExtraviado();
        situacao = SituacaoItemEmpenho.EXTRAVIADO;
        extraviadoEm = LocalDateTime.now();
        extravioRegistradoPor = registradoPor;
    }

    private void exigirEmpenhado() {
        if (situacao != SituacaoItemEmpenho.EMPENHADO) {
            throw new IllegalStateException("O item não está empenhado ou já foi encerrado.");
        }
    }
}
