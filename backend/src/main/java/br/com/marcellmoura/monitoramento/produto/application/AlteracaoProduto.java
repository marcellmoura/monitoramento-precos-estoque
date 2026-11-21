package br.com.marcellmoura.monitoramento.produto.application;

import java.math.BigDecimal;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

public record AlteracaoProduto(long versao, String nome, boolean alterarCategoria, String categoria,
        Unidade unidade, BigDecimal precoVenda, Boolean ativo, BigDecimal estoqueAtual, BigDecimal estoqueMinimo) {}
