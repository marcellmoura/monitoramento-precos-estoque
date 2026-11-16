package br.com.marcellmoura.monitoramento.produto.application;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.produto.domain.Produto;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;
import br.com.marcellmoura.monitoramento.produto.persistence.ProdutoRepository;

@Service
@Transactional(readOnly = true)
public class ProdutoService {
    private final ProdutoRepository produtos;
    public ProdutoService(ProdutoRepository produtos) { this.produtos = produtos; }
    public Page<Produto> listar(int pagina, int tamanho) {
        return produtos.findAll(PageRequest.of(pagina, tamanho, Sort.by("id")));
    }
    public Produto consultar(Long id) {
        return produtos.findById(id).orElseThrow(ProdutoNaoEncontradoException::new);
    }
    @Transactional
    public Produto cadastrar(String nome, String categoria, Unidade unidade, BigDecimal compra, BigDecimal venda) {
        return produtos.saveAndFlush(new Produto(nome, categoria, unidade, compra, venda));
    }
}
