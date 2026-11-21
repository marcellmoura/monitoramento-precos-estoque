package br.com.marcellmoura.monitoramento.produto.domain;

public class ProdutoEdicaoInvalidaException extends IllegalArgumentException {
    public ProdutoEdicaoInvalidaException(String mensagem) { super(mensagem); }
}
