package br.com.marcellmoura.monitoramento.produto.application;

public class ProdutoNaoEncontradoException extends RuntimeException {
    public ProdutoNaoEncontradoException() { super("Produto não encontrado."); }
}
