package br.com.marcellmoura.monitoramento.importacao.domain;

import br.com.marcellmoura.monitoramento.produto.domain.Produto;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

public record VinculoItem(long id, int linha, ProdutoCatalogo produto, Identidade identidade) {
    public record ProdutoCatalogo(long id, String codigo, String nome, Unidade unidade) {
        public static ProdutoCatalogo de(Produto produto) {
            return new ProdutoCatalogo(produto.getId(), produto.getCodigo(), produto.getNome(), produto.getUnidade());
        }
    }

    public record Identidade(String referencia, String descricao, Unidade unidade, String embalagem) {
        public boolean mesmaApresentacao(Identidade outra) {
            if (unidade != outra.unidade || !embalagem.equalsIgnoreCase(outra.embalagem)) return false;
            return referencia == null ? outra.referencia == null && descricao.equalsIgnoreCase(outra.descricao)
                    : referencia.equals(outra.referencia);
        }
    }

    public record Situacao(int linha, Long conferencia, long revisaoItem, Identidade identidade, VinculoItem vinculo,
            boolean revalidar) {}
}
