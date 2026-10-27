package br.com.sicomat.seguranca;

import br.com.sicomat.equipe.Recebedor;

/** Contrato para integrar um provedor de reconhecimento facial. O provedor define a escala do score. */
public interface VerificadorFacial {

    void cadastrar(Recebedor recebedor, byte[] imagem);

    double calcularScore(Recebedor recebedor, byte[] imagem);

    boolean aprovado(double score);
}

