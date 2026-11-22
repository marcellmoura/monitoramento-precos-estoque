package br.com.marcellmoura.monitoramento.importacao.domain;

import java.math.BigDecimal;
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

    public record Linha(int linha, Estado estado, List<String> motivos, BigDecimal preco, Confirmado confirmado,
            Conflito conflito, Classificacao classificacao) {
        public static Linha pendente(int linha, List<String> motivos) {
            return new Linha(linha, Estado.PENDENTE, motivos, null, null, null, Classificacao.PENDENTE);
        }

        public static Linha emConflito(int linha, String motivo, Conflito conflito) {
            return new Linha(linha, Estado.PENDENTE, List.of(motivo), null, null, conflito, Classificacao.PENDENTE);
        }

        public static Linha pronto(int linha, BigDecimal preco) {
            return new Linha(linha, Estado.PRONTO, List.of("Aguardando confirmação do preço."), preco, null, null, Classificacao.PENDENTE);
        }

        public static Linha confirmado(Confirmado registro) {
            return new Linha(registro.linha(), Estado.CONFIRMADO, List.of(), registro.preco(), registro, null,
                    classificar(registro));
        }

        private static Classificacao classificar(Confirmado registro) {
            if (registro.substituido()) return null;
            if (registro.anterior() == null) return Classificacao.NOVO;
            int comparacao = registro.preco().compareTo(registro.anterior().preco());
            if (comparacao > 0) return Classificacao.AUMENTOU;
            return comparacao < 0 ? Classificacao.DIMINUIU : Classificacao.SEM_ALTERACAO;
        }
    }
}
