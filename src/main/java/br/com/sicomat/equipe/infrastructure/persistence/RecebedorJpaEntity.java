package br.com.sicomat.equipe.infrastructure.persistence;

import br.com.sicomat.seguranca.CodificadorSenha;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "recebedor")
public class RecebedorJpaEntity {

    private static final int MAXIMO_FALHAS = 5;
    private static final Duration JANELA_FALHAS = Duration.ofMinutes(5);
    private static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(5);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String setor;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 40)
    private String matricula;

    @Column(name = "pin_hash", nullable = false, length = 255)
    private String pinHash;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "falhas_pin", nullable = false)
    private int falhasPin;

    @Column(name = "inicio_janela_falhas")
    private Instant inicioJanelaFalhas;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    protected RecebedorJpaEntity() {
    }

    public RecebedorJpaEntity(String setor, String nome, String matricula, String pinHash) {
        this.setor = textoObrigatorio(setor, "setor");
        this.nome = textoObrigatorio(nome, "nome");
        this.matricula = textoObrigatorio(matricula, "matrícula");
        if (!CodificadorSenha.formatoValido(pinHash)) {
            throw new IllegalArgumentException("O PIN precisa estar armazenado em formato de hash válido.");
        }
        this.pinHash = pinHash;
    }

    public Long getId() { return id; }
    public String getSetor() { return setor; }
    public String getNome() { return nome; }
    public String getMatricula() { return matricula; }
    public boolean isAtivo() { return ativo; }
    public boolean estaBloqueado(Instant agora) { return bloqueadoAte != null && agora.isBefore(bloqueadoAte); }

    public boolean verificarPin(String pin, Instant agora) {
        if (!ativo || estaBloqueado(agora)) return false;
        if (bloqueadoAte != null) limparFalhas();

        boolean correto = CodificadorSenha.verificar(pin, pinHash);
        if (correto) {
            limparFalhas();
            return true;
        }
        registrarFalha(agora);
        return false;
    }

    public void redefinirPin(String novoHash) {
        if (!CodificadorSenha.formatoValido(novoHash)) {
            throw new IllegalArgumentException("O PIN precisa estar armazenado em formato de hash válido.");
        }
        pinHash = novoHash;
        limparFalhas();
    }

    public void inativar() {
        ativo = false;
        limparFalhas();
    }

    private void registrarFalha(Instant agora) {
        if (inicioJanelaFalhas == null || !agora.isBefore(inicioJanelaFalhas.plus(JANELA_FALHAS))) {
            inicioJanelaFalhas = agora;
            falhasPin = 0;
        }
        falhasPin++;
        if (falhasPin >= MAXIMO_FALHAS) {
            bloqueadoAte = agora.plus(DURACAO_BLOQUEIO);
            inicioJanelaFalhas = null;
            falhasPin = 0;
        }
    }

    private void limparFalhas() {
        falhasPin = 0;
        inicioJanelaFalhas = null;
        bloqueadoAte = null;
    }

    private static String textoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("O campo " + campo + " é obrigatório.");
        return valor.trim();
    }
}
