package br.com.marcellmoura.monitoramento.importacao.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.BaseComparacao;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.Classificacao;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.Confirmado;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.Linha;
import static org.assertj.core.api.Assertions.assertThat;

class ConfirmacaoImportacaoTest {
    private static final LocalDate DATA = LocalDate.of(2026, 11, 21);
    private static final Instant CONFIRMACAO = Instant.parse("2026-11-21T12:00:00Z");

    @ParameterizedTest
    @CsvSource({
        "33.00, 35.00, AUMENTOU, 2.00, 6.06",
        "15.00, 14.00, DIMINUIU, 1.00, -6.67",
        "38.00, 38, SEM_ALTERACAO, 0.00, 0.00",
        "32.00, 33.00, AUMENTOU, 1.00, 3.13",
        "32.00, 31.00, DIMINUIU, 1.00, -3.13",
        "0.01, 999999999999.99, AUMENTOU, 999999999999.98, 9999999999999800.00"
    })
    void calculaVariacaoComBasePositiva(String anterior, String atual, Classificacao esperado,
            String diferenca, String percentual) {
        var base = new BaseComparacao(1, 1, new BigDecimal(anterior), DATA.minusDays(1));
        var linha = Linha.confirmado(new Confirmado(2, 1, 1, 1, new BigDecimal(atual), DATA, CONFIRMACAO, false, base));

        assertThat(linha.classificacao()).isEqualTo(esperado);
        assertThat(linha.variacao().diferencaAbsoluta()).isEqualByComparingTo(diferenca);
        assertThat(linha.variacao().percentual()).isEqualTo(percentual);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-5.00"})
    void exigeBasePositivaParaCalcularVariacao(String anterior) {
        var base = new BaseComparacao(1, 1, new BigDecimal(anterior), DATA.minusDays(1));
        var linha = Linha.confirmado(new Confirmado(2, 1, 1, 1, new BigDecimal("32.00"), DATA, CONFIRMACAO, false, base));

        assertThat(linha.variacao()).isNull();
    }

    @Test
    void distingueNovaOcorrenciaDePrecoSubstituido() {
        var preco = new BigDecimal("32.00");
        var novo = Linha.confirmado(new Confirmado(2, 1, 1, 1, preco, DATA, CONFIRMACAO, false, null));
        var substituido = Linha.confirmado(new Confirmado(3, 1, 1, 1, preco, DATA, CONFIRMACAO, true, null));

        assertThat(novo.classificacao()).isEqualTo(Classificacao.NOVO);
        assertThat(novo.variacao()).isNull();
        assertThat(substituido.classificacao()).isNull();
        assertThat(substituido.variacao()).isNull();
    }

    @Test
    void classificaLinhasAindaNaoConfirmadasComMotivo() {
        var pendente = Linha.pendente(1, List.of("Confirme a associação ao produto do catálogo."));
        var pronto = Linha.pronto(2, new BigDecimal("32.00"));

        assertThat(pendente.classificacao()).isEqualTo(Classificacao.PENDENTE);
        assertThat(pendente.motivos()).containsExactly("Confirme a associação ao produto do catálogo.");
        assertThat(pronto.classificacao()).isEqualTo(Classificacao.PENDENTE);
        assertThat(pronto.motivos()).containsExactly("Aguardando confirmação do preço.");
        assertThat(pendente.variacao()).isNull();
        assertThat(pronto.variacao()).isNull();
    }
}
