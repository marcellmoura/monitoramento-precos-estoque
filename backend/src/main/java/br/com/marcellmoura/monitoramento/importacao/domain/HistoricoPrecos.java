package br.com.marcellmoura.monitoramento.importacao.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

public record HistoricoPrecos(List<Registro> itens, int pagina, int tamanho, long total, int paginas) {
    public record Filtros(Long produtoId, Long fornecedorId, Long tentativa, LocalDate inicio, LocalDate fim) {}

    public record Identificacao(String codigo, String nome) {}

    public record Origem(long tentativa, int linha, String arquivo) {}

    public record VinculoSubstituicao(long tentativa, int linha, BigDecimal preco, String motivo) {}

    public record Registro(long id, Identificacao produto, Identificacao fornecedor, Unidade unidade, String embalagem,
            String modalidade, BigDecimal preco, LocalDate dataComercial, Integer ordem, Origem origem,
            VinculoSubstituicao substituiu, VinculoSubstituicao substituidoPor) {}
}
