package br.com.marcellmoura.monitoramento.importacao.extracao;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class InterpretadorCotacaoTest {
    private final InterpretadorCotacao interpretador = new InterpretadorCotacao();

    @Test void preservaLinhaIncompletaEExtraiAsDemais() throws Exception {
        var previa = interpretador.interpretar("Descrição\tATACADO\tVAREJO\nFeijão\t19,00\t22,00\nLinha quebrada\nArroz\t0\t25");
        assertThat(previa.itens()).hasSize(3);
        assertThat(previa.itens().get(1).dados()).isNull();
        assertThat(previa.itens().get(1).textoOriginal()).isEqualTo("Linha quebrada");
        assertThat(previa.itens().get(2).pendencias()).contains("Preço ausente ou inválido em ATACADO.");
    }

    @Test void cabecalhoSemLinhaLegivelEUmaFalha() {
        assertThatThrownBy(() -> interpretador.interpretar("Descrição\tATACADO\tVAREJO\nSem preços"))
                .isInstanceOf(ExtracaoPdfException.class).hasMessageContaining("Nenhum item");
    }
    @Test void reconheceDataComCabecalhoAcentuadoEmMaiusculas() throws Exception {
        var previa = interpretador.interpretar("DATA DE REFERÊNCIA: 01/11/2026\nDescrição\tATACADO\tVAREJO\nFeijão\t19\t22");
        assertThat(previa.dataReferencia()).isEqualTo("01/11/2026");
    }

}
