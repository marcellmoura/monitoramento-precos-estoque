package br.com.marcellmoura.monitoramento.produto.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "produtos")
public class Produto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String nome;
    @Column(length = 100)
    private String categoria;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 2)
    private Unidade unidade;
    @Column(name = "preco_compra", precision = 19, scale = 2)
    private BigDecimal precoCompra;
    @Column(name = "preco_venda", nullable = false, precision = 19, scale = 2)
    private BigDecimal precoVenda;
    @Column(name = "estoque_atual", nullable = false, precision = 19, scale = 3)
    private BigDecimal estoqueAtual;
    @Column(name = "estoque_minimo", nullable = false, precision = 19, scale = 3)
    private BigDecimal estoqueMinimo;
    @Column(nullable = false)
    private boolean ativo;
    @Version
    private long versao;
    @Column(name = "unidade_bloqueada", nullable = false)
    private boolean unidadeBloqueada;

    protected Produto() {}
    public Produto(String nome, String categoria, Unidade unidade, BigDecimal compra, BigDecimal venda) {
        alterarNome(nome);
        alterarCategoria(categoria);
        this.unidade = Objects.requireNonNull(unidade);
        this.precoCompra = compra == null ? null : validarPreco(compra);
        this.precoVenda = validarPreco(Objects.requireNonNull(venda));
        estoqueAtual = BigDecimal.ZERO.setScale(3);
        estoqueMinimo = BigDecimal.ZERO.setScale(3);
        ativo = true;
    }

    public void alterarNome(String nome) {
        String texto = Objects.requireNonNull(nome).strip();
        if (texto.isEmpty() || texto.length() > 255) throw new ProdutoEdicaoInvalidaException("Informe um nome de até 255 caracteres.");
        this.nome = texto;
    }

    public void alterarCategoria(String categoria) {
        String texto = categoria == null || categoria.isBlank() ? null : categoria.strip();
        if (texto != null && texto.length() > 100) throw new ProdutoEdicaoInvalidaException("Informe uma categoria de até 100 caracteres.");
        this.categoria = texto;
    }

    public void alterarVenda(BigDecimal venda) { precoVenda = validarPreco(venda); }

    public void alterarEstado(boolean ativo) { this.ativo = ativo; }

    public void bloquearUnidade() { unidadeBloqueada = true; }

    public boolean podeTrocarUnidade() {
        return !unidadeBloqueada && estoqueAtual.signum() == 0 && estoqueMinimo.signum() == 0;
    }

    public void alterarUnidade(Unidade novaUnidade) {
        if (unidade == novaUnidade) return;
        if (!podeTrocarUnidade()) throw new ProdutoEdicaoInvalidaException("A troca de unidade exige estoque zerado e produto sem histórico ou vínculos.");
        unidade = novaUnidade;
    }

    public boolean ajustarEstoque(BigDecimal atual, BigDecimal minimo) {
        BigDecimal novoAtual = atual == null ? estoqueAtual : validarQuantidade(atual);
        BigDecimal novoMinimo = minimo == null ? estoqueMinimo : validarQuantidade(minimo);
        if (novoAtual.compareTo(estoqueAtual) == 0 && novoMinimo.compareTo(estoqueMinimo) == 0) return false;
        estoqueAtual = novoAtual;
        estoqueMinimo = novoMinimo;
        bloquearUnidade();
        return true;
    }

    private BigDecimal validarQuantidade(BigDecimal valor) {
        if (valor.signum() < 0 || valor.scale() > 3 || valor.precision() - valor.scale() > 16) {
            throw new ProdutoEdicaoInvalidaException("Informe estoques não negativos, com até três casas decimais.");
        }
        if (unidade == Unidade.UN && valor.stripTrailingZeros().scale() > 0) {
            throw new ProdutoEdicaoInvalidaException("Produtos em UN exigem estoques inteiros.");
        }
        return valor.setScale(3, RoundingMode.UNNECESSARY);
    }

    private BigDecimal validarPreco(BigDecimal valor) {
        if (valor.signum() < 0 || valor.scale() > 2 || valor.precision() - valor.scale() > 17) {
            throw new ProdutoEdicaoInvalidaException("Preço inválido.");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }
    public Long getId() { return id; }
    public String getCodigo() { return gerarCodigo(id); }

    public static String gerarCodigo(Long id) { return String.format(Locale.ROOT, "PRD-%06d", id); }
    public String getNome() { return nome; }
    public String getCategoria() { return categoria; }
    public Unidade getUnidade() { return unidade; }
    public BigDecimal getPrecoCompra() { return precoCompra; }
    public BigDecimal getPrecoVenda() { return precoVenda; }
    public BigDecimal getEstoqueAtual() { return estoqueAtual; }
    public BigDecimal getEstoqueMinimo() { return estoqueMinimo; }
    public boolean isAtivo() { return ativo; }
    public long getVersao() { return versao; }
}
