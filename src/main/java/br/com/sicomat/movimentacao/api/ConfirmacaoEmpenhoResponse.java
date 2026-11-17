package br.com.sicomat.movimentacao.api;

public record ConfirmacaoEmpenhoResponse(boolean confirmado, String mensagem, EmpenhoResponse empenho) {
}
