package br.com.marcellmoura.monitoramento.usuario.application;

public class FalhaUsuarioException extends RuntimeException {
    public enum Motivo { NAO_ENCONTRADO, EMAIL_DUPLICADO, ULTIMO_ADMINISTRADOR, ACESSO_REVOGADO }
    private final Motivo motivo;
    public FalhaUsuarioException(Motivo motivo) { super(motivo.name()); this.motivo = motivo; }
    public Motivo getMotivo() { return motivo; }
}
