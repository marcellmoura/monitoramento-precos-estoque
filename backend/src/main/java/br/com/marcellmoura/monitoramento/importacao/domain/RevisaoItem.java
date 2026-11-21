package br.com.marcellmoura.monitoramento.importacao.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

public record RevisaoItem(long id, int linha, long atorId, Instant revisadaEm, Dados dados, List<String> pendencias) {
    public record Dados(String referencia, String descricao, Unidade unidade, String embalagem, BigDecimal quantidade,
            String coluna, BigDecimal preco, String motivo) {}

    public static List<String> verificar(Dados dados) {
        var pendencias = new ArrayList<String>();
        if (dados.unidade() == null) pendencias.add("Informe a unidade de compra.");
        if (dados.embalagem() == null) pendencias.add("Informe a embalagem de compra.");
        if (dados.preco() == null) pendencias.add("Informe o preço de compra confirmado para esta unidade e embalagem.");
        return List.copyOf(pendencias);
    }
}
