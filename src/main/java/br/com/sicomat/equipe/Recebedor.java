package br.com.sicomat.equipe;

import br.com.sicomat.seguranca.CodificadorSenha;
import java.time.LocalDateTime;
import java.util.Arrays;

public class Recebedor {

    private final String nome;
    private final String matricula;
    private String pinHash;
    private byte[] templateFacial;
    private LocalDateTime consentimentoEm;
    private boolean ativo = true;

    public Recebedor(String nome, String matricula, String pinHash) {
        this.nome = textoObrigatorio(nome, "nome");
        this.matricula = textoObrigatorio(matricula, "matrícula");
        this.pinHash = textoObrigatorio(pinHash, "hash do PIN");
    }

    public String getNome() { return nome; }
    public String getMatricula() { return matricula; }
    public synchronized boolean isAtivo() { return ativo; }
    public synchronized LocalDateTime getConsentimentoEm() { return consentimentoEm; }
    public synchronized boolean possuiFaceCadastrada() { return templateFacial != null && templateFacial.length > 0; }

    public synchronized boolean verificarPin(String pin) {
        return ativo && CodificadorSenha.verificar(pin, pinHash);
    }

    /** Chamado pelo provedor biométrico após gerar um template, nunca com a foto original. */
    public synchronized void registrarTemplateFacial(byte[] template, LocalDateTime consentidoEm) {
        if (!ativo) throw new IllegalStateException("O recebedor está inativo.");
        if (template == null || template.length == 0) throw new IllegalArgumentException("O template facial não pode ficar vazio.");
        if (consentidoEm == null || consentidoEm.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Informe uma data válida para o consentimento.");
        this.templateFacial = Arrays.copyOf(template, template.length);
        this.consentimentoEm = consentidoEm;
    }

    public synchronized byte[] obterTemplateFacial() {
        return templateFacial == null ? null : Arrays.copyOf(templateFacial, templateFacial.length);
    }

    public synchronized void redefinirPin(String novoPin) {
        if (novoPin == null || novoPin.isBlank()) throw new IllegalArgumentException("O PIN não pode ficar vazio.");
        pinHash = CodificadorSenha.codificar(novoPin);
    }

    public synchronized void inativar() { ativo = false; }
    public synchronized void ativar() { ativo = true; }

    private static String textoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("O campo " + campo + " é obrigatório.");
        return valor.trim();
    }
}

