package br.com.sicomat.movimentacao.infrastructure.persistence;

import br.com.sicomat.equipe.infrastructure.persistence.EquipeJpaEntity;
import br.com.sicomat.equipe.infrastructure.persistence.RecebedorJpaEntity;
import br.com.sicomat.movimentacao.StatusEmpenho;
import br.com.sicomat.usuario.PerfilUsuario;
import br.com.sicomat.usuario.infrastructure.persistence.UsuarioJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "empenho")
public class EmpenhoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String setor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @jakarta.persistence.JoinColumn(name = "operador_id", nullable = false)
    private UsuarioJpaEntity operador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @jakarta.persistence.JoinColumn(name = "recebedor_id", nullable = false)
    private RecebedorJpaEntity recebedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @jakarta.persistence.JoinColumn(name = "equipe_id", nullable = false)
    private EquipeJpaEntity equipe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusEmpenho status = StatusEmpenho.PENDENTE;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora = Instant.now();

    @Column(nullable = false, length = 500)
    private String observacao;

    @Column(name = "confirmado_em")
    private Instant confirmadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "confirmado_por")
    private UsuarioJpaEntity confirmadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "cancelado_por")
    private UsuarioJpaEntity canceladoPor;

    @Column(name = "confirmado_por_pin", nullable = false)
    private boolean confirmadoPorPin;

    @Column(name = "justificativa_pin", length = 500)
    private String justificativaPin;

    @Version
    @Column(nullable = false)
    private long versao;

    @OneToMany(mappedBy = "empenho", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ItemEmpenhoJpaEntity> itens = new ArrayList<>();

    protected EmpenhoJpaEntity() {
    }

    public EmpenhoJpaEntity(String setor, UsuarioJpaEntity operador, RecebedorJpaEntity recebedor,
                            EquipeJpaEntity equipe, String observacao) {
        this.setor = textoObrigatorio(setor, "setor");
        this.operador = Objects.requireNonNull(operador, "O operador é obrigatório.");
        if (operador.getPerfil() != PerfilUsuario.OPERADOR_ESTOQUE || !operador.isAtivo()) {
            throw new IllegalArgumentException("O empenho deve ser registrado por um operador ativo.");
        }
        this.recebedor = Objects.requireNonNull(recebedor, "O recebedor é obrigatório.");
        if (!recebedor.isAtivo()) throw new IllegalArgumentException("O recebedor está inativo.");
        this.equipe = Objects.requireNonNull(equipe, "A equipe é obrigatória.");
        if (!equipe.isAtivo()) throw new IllegalArgumentException("A equipe está inativa.");
        this.observacao = observacao == null ? "" : observacao.trim();
    }

    public Long getId() { return id; }
    public String getSetor() { return setor; }
    public UsuarioJpaEntity getOperador() { return operador; }
    public RecebedorJpaEntity getRecebedor() { return recebedor; }
    public EquipeJpaEntity getEquipe() { return equipe; }
    public StatusEmpenho getStatus() { return status; }
    public Instant getDataHora() { return dataHora; }
    public String getObservacao() { return observacao; }
    public Instant getConfirmadoEm() { return confirmadoEm; }
    public UsuarioJpaEntity getConfirmadoPor() { return confirmadoPor; }
    public UsuarioJpaEntity getCanceladoPor() { return canceladoPor; }
    public boolean isConfirmadoPorPin() { return confirmadoPorPin; }
    public String getJustificativaPin() { return justificativaPin; }
    public List<ItemEmpenhoJpaEntity> getItens() { return Collections.unmodifiableList(itens); }

    public void adicionarItem(ItemEmpenhoJpaEntity item) {
        Objects.requireNonNull(item, "O item do empenho é obrigatório.");
        itens.add(item);
    }

    public void confirmarPorPin(UsuarioJpaEntity operador, String justificativa) {
        exigirPendente();
        confirmadoEm = Instant.now();
        confirmadoPor = Objects.requireNonNull(operador, "O operador é obrigatório.");
        confirmadoPorPin = true;
        justificativaPin = textoObrigatorio(justificativa, "justificativa");
        status = StatusEmpenho.ABERTO;
    }

    public void cancelar(UsuarioJpaEntity operador) {
        exigirPendente();
        canceladoPor = Objects.requireNonNull(operador, "O operador é obrigatório.");
        status = StatusEmpenho.CANCELADO;
    }

    public void recalcularStatus() {
        if (status == StatusEmpenho.PENDENTE || status == StatusEmpenho.CANCELADO) return;
        boolean pendente = itens.stream().anyMatch(item -> item.getSituacao() == br.com.sicomat.movimentacao.SituacaoItemEmpenho.EMPENHADO);
        boolean finalizado = itens.stream().anyMatch(item -> item.getSituacao() == br.com.sicomat.movimentacao.SituacaoItemEmpenho.DEVOLVIDO
                || item.getSituacao() == br.com.sicomat.movimentacao.SituacaoItemEmpenho.EXTRAVIADO);
        boolean extraviado = itens.stream().anyMatch(item -> item.getSituacao() == br.com.sicomat.movimentacao.SituacaoItemEmpenho.EXTRAVIADO);
        if (pendente) {
            status = finalizado ? StatusEmpenho.DEVOLVIDO_PARCIALMENTE : StatusEmpenho.ABERTO;
        } else {
            status = extraviado ? StatusEmpenho.EXTRAVIADO : StatusEmpenho.ENCERRADO;
        }
    }

    private void exigirPendente() {
        if (status != StatusEmpenho.PENDENTE) throw new IllegalStateException("O empenho não está pendente.");
    }

    private static String textoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("O campo " + campo + " é obrigatório.");
        return valor.trim();
    }
}
