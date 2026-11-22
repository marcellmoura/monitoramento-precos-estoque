package br.com.marcellmoura.monitoramento.importacao.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ConfirmacaoImportacao(List<Linha> itens) {
    public enum Estado { PENDENTE, PRONTO, CONFIRMADO }

    public enum Classificacao { AUMENTOU, DIMINUIU, SEM_ALTERACAO, NOVO, PENDENTE }

    public enum Operacao { ANTES, DEPOIS, SUBSTITUIR }

    public record Resolucao(Operacao operacao, long precoReferencia, String motivo, String revisao) {}

    public record PrecoAnterior(long id, long tentativa, BigDecimal preco, int ordem) {}

    public record Conflito(BigDecimal preco, LocalDate dataComercial, List<PrecoAnterior> anteriores, String revisao) {}

    public record BaseComparacao(long id, long tentativa, BigDecimal preco, LocalDate dataComercial) {}

    public record Confirmado(long id, int linha, long produtoId, long vinculoOrigem, BigDecimal preco, LocalDate dataComercial,
            Instant confirmadaEm, boolean substituido, BaseComparacao anterior) {}

    public record Variacao(BigDecimal diferencaAbsoluta, String percentual) {}

    public record Linha(int linha, Estado estado, List<String> motivos, BigDecimal preco, Confirmado confirmado,
            Conflito conflito, Classificacao classificacao, Variacao variacao) {
        public static Linha pendente(int linha, List<String> motivos) {
            return new Linha(linha, Estado.PENDENTE, motivos, null, null, null, Classificacao.PENDENTE, null);
        }

        public static Linha emConflito(int linha, String motivo, Conflito conflito) {
            return new Linha(linha, Estado.PENDENTE, List.of(motivo), null, null, conflito, Classificacao.PENDENTE, null);
        }

        public static Linha pronto(int linha, BigDecimal preco) {
            return new Linha(linha, Estado.PRONTO, List.of("Aguardando confirmação do preço."), preco, null, null, Classificacao.PENDENTE, null);
        }

        public static Linha confirmado(Confirmado registro) {
            if (registro.substituido()) return confirmado(registro, null, null);
            var anterior = registro.anterior();
            if (anterior == null || anterior.preco().signum() <= 0) return confirmado(registro, Classificacao.NOVO, null);

            var diferenca = registro.preco().subtract(anterior.preco());
            var percentual = diferenca.movePointRight(2).divide(anterior.preco(), 2, RoundingMode.HALF_UP);
            return confirmado(registro, classificar(diferenca), new Variacao(diferenca.abs(), percentual.toPlainString()));
        }

        private static Linha confirmado(Confirmado registro, Classificacao classificacao, Variacao variacao) {
            return new Linha(registro.linha(), Estado.CONFIRMADO, List.of(), registro.preco(), registro, null,
                    classificacao, variacao);
        }

        private static Classificacao classificar(BigDecimal diferenca) {
            int sinal = diferenca.signum();
            if (sinal > 0) return Classificacao.AUMENTOU;
            return sinal < 0 ? Classificacao.DIMINUIU : Classificacao.SEM_ALTERACAO;
        }
    }
}
