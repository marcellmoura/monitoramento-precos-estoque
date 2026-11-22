package br.com.marcellmoura.monitoramento.importacao.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ConfirmacaoImportacao(List<Linha> itens) {
    public enum Estado { PENDENTE, PRONTO, CONFIRMADO }

    public record Confirmado(long id, int linha, long produtoId, long vinculoOrigem, BigDecimal preco, LocalDate dataComercial,
            Instant confirmadaEm) {}

    public record Linha(int linha, Estado estado, List<String> motivos, BigDecimal preco, Confirmado confirmado) {
        public static Linha pendente(int linha, List<String> motivos) {
            return new Linha(linha, Estado.PENDENTE, motivos, null, null);
        }

        public static Linha pronto(int linha, BigDecimal preco) {
            return new Linha(linha, Estado.PRONTO, List.of(), preco, null);
        }

        public static Linha confirmado(Confirmado registro) {
            return new Linha(registro.linha(), Estado.CONFIRMADO, List.of(), registro.preco(), registro);
        }
    }
}
