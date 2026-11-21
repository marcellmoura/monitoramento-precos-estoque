package br.com.marcellmoura.monitoramento.importacao.domain;

import java.time.LocalDate;
import java.util.List;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

public record ConferenciaCotacao(List<String> colunas, LocalDate dataSugerida, Revisao confirmada, Base sugestao,
        List<List<String>> pendenciasComerciais) {
    public static boolean unidadeCompativel(String layout, String coluna, Unidade unidade) {
        return !"KG_FARDO".equals(layout) || !"Preço KG".equals(coluna) || unidade == Unidade.KG;
    }

    public record Base(String coluna, Unidade unidade, String embalagem) {}
    public record Revisao(long id, Base base, LocalDate dataComercial) {}
}
