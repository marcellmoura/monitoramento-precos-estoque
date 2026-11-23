package br.com.marcellmoura.monitoramento.produto.application;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.produto.domain.Produto;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;
import br.com.marcellmoura.monitoramento.produto.persistence.ProdutoRepository;

@Service
@Transactional(readOnly = true)
public class ProdutoService {
    private final ProdutoRepository produtos;
    private final JdbcTemplate jdbc;

    public ProdutoService(ProdutoRepository produtos, JdbcTemplate jdbc) {
        this.produtos = produtos;
        this.jdbc = jdbc;
    }
    public Page<Produto> listar(int pagina, int tamanho, String busca, Unidade unidade, Boolean ativo) {
        return produtos.buscar(busca.strip(), unidade, ativo, PageRequest.of(pagina, tamanho, Sort.by("id")));
    }
    public Produto consultar(Long id) {
        return produtos.findById(id).orElseThrow(ProdutoNaoEncontradoException::new);
    }
    @Transactional
    public Produto cadastrar(String nome, String categoria, Unidade unidade, BigDecimal compra, BigDecimal venda) {
        return produtos.saveAndFlush(new Produto(nome, categoria, unidade, compra, venda));
    }

    @Transactional
    public Produto editar(long id, long atorId, AlteracaoProduto alteracao) {
        Produto produto = consultar(id);
        if (produto.getVersao() != alteracao.versao()) throw new ObjectOptimisticLockingFailureException(Produto.class, id);
        if (alteracao.nome() != null) produto.alterarNome(alteracao.nome());
        if (alteracao.alterarCategoria()) produto.alterarCategoria(alteracao.categoria());
        if (alteracao.unidade() != null) produto.alterarUnidade(alteracao.unidade());
        if (alteracao.precoVenda() != null) produto.alterarVenda(alteracao.precoVenda());
        if (alteracao.ativo() != null) produto.alterarEstado(alteracao.ativo());

        BigDecimal atualAnterior = produto.getEstoqueAtual();
        BigDecimal minimoAnterior = produto.getEstoqueMinimo();
        boolean estoqueAlterado = produto.ajustarEstoque(alteracao.estoqueAtual(), alteracao.estoqueMinimo());
        produtos.flush();
        if (estoqueAlterado) registrarAjuste(produto, atorId, atualAnterior, minimoAnterior);
        return produto;
    }

    private void registrarAjuste(Produto produto, long atorId, BigDecimal atualAnterior, BigDecimal minimoAnterior) {
        jdbc.update("""
                INSERT INTO historico_ajustes_estoque
                    (produto_id, ator_id, unidade, atual_anterior, atual_novo, minimo_anterior, minimo_novo)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, produto.getId(), atorId, produto.getUnidade().name(), atualAnterior, produto.getEstoqueAtual(),
                minimoAnterior, produto.getEstoqueMinimo());
    }
}
