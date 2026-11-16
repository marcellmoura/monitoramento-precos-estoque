package br.com.marcellmoura.monitoramento.produto.domain;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class ProdutoTest {
    @ParameterizedTest @EnumSource(Unidade.class)
    void iniciaAtivoComEstoquesZeroECompraAusente(Unidade unidade) {
        var produto = new Produto("  Arroz  ", " ", unidade, null, BigDecimal.ZERO);
        assertThat(produto.getNome()).isEqualTo("Arroz");
        assertThat(produto.getCategoria()).isNull();
        assertThat(produto.getUnidade()).isEqualTo(unidade);
        assertThat(produto.getPrecoCompra()).isNull();
        assertThat(produto.getPrecoVenda()).isEqualByComparingTo("0.00");
        assertThat(produto.getEstoqueAtual()).isEqualByComparingTo("0");
        assertThat(produto.getEstoqueMinimo()).isEqualByComparingTo("0");
        assertThat(produto.isAtivo()).isTrue();
    }
    @Test void preservaMaiorPrecoSemPerdaDeCentavosENormalizaCategoria() {
        var preco = new BigDecimal("99999999999999999.99");
        var produto = new Produto("Produto", "  Alimentos  ", Unidade.KG, preco, preco);
        assertThat(produto.getPrecoCompra().toPlainString()).isEqualTo("99999999999999999.99");
        assertThat(produto.getPrecoVenda()).isEqualTo(preco);
        assertThat(produto.getCategoria()).isEqualTo("Alimentos");
    }
    @ParameterizedTest @ValueSource(strings={"-0.01","1.001","100000000000000000.00"})
    void recusaPrecoNegativoFracaoExcedenteOuInteiroAcimaDoLimite(String preco) {
        var valor = new BigDecimal(preco);
        assertThatThrownBy(() -> new Produto("Teste",null,Unidade.UN,valor,BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Produto("Teste",null,Unidade.UN,null,valor)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void recusaNomeVazioOuLongoECategoriaExcedente() {
        assertThatThrownBy(() -> new Produto(" ",null,Unidade.UN,null,BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Produto("a".repeat(256),null,Unidade.UN,null,BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Produto("Nome","a".repeat(101),Unidade.UN,null,BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
    }
}
