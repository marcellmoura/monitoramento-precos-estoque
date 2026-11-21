package br.com.marcellmoura.monitoramento.importacao.application;

public class ImportacaoException extends RuntimeException {
    private final int status;
    private final String codigo;

    ImportacaoException(int status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public int status() { return status; }
    public String codigo() { return codigo; }
}
