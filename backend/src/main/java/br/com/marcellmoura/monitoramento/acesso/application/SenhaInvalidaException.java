package br.com.marcellmoura.monitoramento.acesso.application;

public class SenhaInvalidaException extends RuntimeException {
    public SenhaInvalidaException(String mensagem) { super(mensagem); }
}
