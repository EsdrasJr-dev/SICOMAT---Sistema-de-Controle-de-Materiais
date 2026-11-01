package br.com.sicomat.estoque.api;

public record MaterialResponse(
        Long id,
        String codigo,
        String nome,
        boolean ativo,
        long quantidadeTotal,
        long quantidadeDisponivel) {
}