package br.com.marcellmoura.monitoramento.produto.persistence;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import br.com.marcellmoura.monitoramento.produto.domain.Produto;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    Page<Produto> findByAtivoTrueAndUnidadeAndNomeContainingIgnoreCase(Unidade unidade, String nome, Pageable pagina);

    @Query("""
            select p from Produto p
            where (:unidade is null or p.unidade = :unidade)
              and (:ativo is null or p.ativo = :ativo)
              and (:busca = ''
                   or locate(lower(:busca), lower(p.nome)) > 0
                   or locate(lower(:busca), concat('prd-',
                       lpad(cast(p.id as String), greatest(6, length(cast(p.id as String))), '0'))) > 0)
            """)
    Page<Produto> buscar(String busca, Unidade unidade, Boolean ativo, Pageable pagina);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id")
    Optional<Produto> buscarParaVinculo(long id);
}
