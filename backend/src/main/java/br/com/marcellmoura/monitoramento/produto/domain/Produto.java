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

    protected Produto() {}
    public Produto(String nome, String categoria, Unidade unidade, BigDecimal compra, BigDecimal venda) {
        this.nome = Objects.requireNonNull(nome).strip();
        if (this.nome.isEmpty() || this.nome.length() > 255) { throw new IllegalArgumentException("Nome inválido."); }
        this.categoria = categoria == null || categoria.isBlank() ? null : categoria.strip();
        if (this.categoria != null && this.categoria.length() > 100) { throw new IllegalArgumentException("Categoria inválida."); }
        this.unidade = Objects.requireNonNull(unidade);
        this.precoCompra = compra == null ? null : validarPreco(compra);
        this.precoVenda = validarPreco(Objects.requireNonNull(venda));
        estoqueAtual = BigDecimal.ZERO.setScale(3);
        estoqueMinimo = BigDecimal.ZERO.setScale(3);
        ativo = true;
    }
    private BigDecimal validarPreco(BigDecimal valor) {
        if (valor.signum() < 0 || valor.scale() > 2 || valor.precision() - valor.scale() > 17) {
            throw new IllegalArgumentException("Preço inválido.");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }
    public Long getId() { return id; }
    public String getCodigo() { return String.format(Locale.ROOT, "PRD-%06d", id); }
    public String getNome() { return nome; }
    public String getCategoria() { return categoria; }
    public Unidade getUnidade() { return unidade; }
    public BigDecimal getPrecoCompra() { return precoCompra; }
    public BigDecimal getPrecoVenda() { return precoVenda; }
    public BigDecimal getEstoqueAtual() { return estoqueAtual; }
    public BigDecimal getEstoqueMinimo() { return estoqueMinimo; }
    public boolean isAtivo() { return ativo; }
}
