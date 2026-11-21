package br.com.marcellmoura.monitoramento.produto.http;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import br.com.marcellmoura.monitoramento.produto.domain.Produto;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;

public sealed interface ProdutoResposta {
    record Dados(Long id, String codigo, String nome, String categoria, Unidade unidade,
            String estoqueAtual, String estoqueMinimo, boolean ativo, long versao, boolean unidadeEditavel) {
        static Dados de(Produto p) {
            return new Dados(p.getId(), p.getCodigo(), p.getNome(), p.getCategoria(), p.getUnidade(),
                    p.getEstoqueAtual().toPlainString(), p.getEstoqueMinimo().toPlainString(), p.isAtivo(),
                    p.getVersao(), p.podeTrocarUnidade());
        }
    }
    record Identificacao(@JsonUnwrapped Dados dados) implements ProdutoResposta {}
    record Venda(@JsonUnwrapped Dados dados, String precoVenda) implements ProdutoResposta {}
    record Completo(@JsonUnwrapped Dados dados, String precoVenda, String precoCompra) implements ProdutoResposta {}

    static ProdutoResposta de(Produto produto, Perfil perfil) {
        var dados = Dados.de(produto);
        return switch (perfil) {
            case ESTOQUE -> new Identificacao(dados);
            case CONSULTA -> new Venda(dados, produto.getPrecoVenda().toPlainString());
            case ADMINISTRADOR, COMPRAS -> new Completo(dados, produto.getPrecoVenda().toPlainString(),
                    produto.getPrecoCompra() == null ? null : produto.getPrecoCompra().toPlainString());
        };
    }
}
