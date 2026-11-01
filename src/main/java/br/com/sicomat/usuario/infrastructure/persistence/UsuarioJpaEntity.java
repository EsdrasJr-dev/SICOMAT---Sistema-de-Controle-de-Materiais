package br.com.sicomat.usuario.infrastructure.persistence;

import br.com.sicomat.usuario.PerfilUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class UsuarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 40)
    private String matricula;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PerfilUsuario perfil;

    @Column(nullable = false, length = 80)
    private String setor;

    @Column(length = 40)
    private String turno;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "falhas_login", nullable = false)
    private int falhasLogin;

    @Column(name = "inicio_janela_falhas")
    private LocalDateTime inicioJanelaFalhas;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    protected UsuarioJpaEntity() {
    }

    public UsuarioJpaEntity(String nome, String matricula, String senhaHash, PerfilUsuario perfil,
                            String setor, String turno) {
        this.nome = nome;
        this.matricula = matricula;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.setor = setor;
        this.turno = turno;
        this.ativo = true;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getMatricula() { return matricula; }
    public String getSenhaHash() { return senhaHash; }
    public PerfilUsuario getPerfil() { return perfil; }
    public String getSetor() { return setor; }
    public String getTurno() { return turno; }
    public boolean isAtivo() { return ativo; }
    public int getFalhasLogin() { return falhasLogin; }
    public LocalDateTime getInicioJanelaFalhas() { return inicioJanelaFalhas; }
    public LocalDateTime getBloqueadoAte() { return bloqueadoAte; }

    public boolean estaBloqueado(LocalDateTime agora) {
        return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
    }

    public void registrarFalha(LocalDateTime agora) {
        if (inicioJanelaFalhas == null || !agora.isBefore(inicioJanelaFalhas.plusMinutes(5))) {
            inicioJanelaFalhas = agora;
            falhasLogin = 0;
        }
        falhasLogin++;
        if (falhasLogin >= 5) {
            bloqueadoAte = agora.plusMinutes(5);
            inicioJanelaFalhas = null;
            falhasLogin = 0;
        }
    }

    public void limparFalhas() {
        falhasLogin = 0;
        inicioJanelaFalhas = null;
        bloqueadoAte = null;
    }

    public void inativar() {
        if (perfil == PerfilUsuario.ADMINISTRADOR_SISTEMA) {
            throw new IllegalStateException("O administrador do sistema não pode ser inativado.");
        }
        ativo = false;
        limparFalhas();
    }
}