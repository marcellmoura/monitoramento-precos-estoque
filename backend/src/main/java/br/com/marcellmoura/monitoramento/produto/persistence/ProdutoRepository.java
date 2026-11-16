package br.com.marcellmoura.monitoramento.produto.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.marcellmoura.monitoramento.produto.domain.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {}
