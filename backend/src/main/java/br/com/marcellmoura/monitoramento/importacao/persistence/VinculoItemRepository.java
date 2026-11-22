package br.com.marcellmoura.monitoramento.importacao.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.Identidade;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.ProdutoCatalogo;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

@Repository
public class VinculoItemRepository {
    private final JdbcTemplate jdbc;

    public VinculoItemRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<VinculoItem> listar(long tentativa) {
        return jdbc.query("""
                SELECT DISTINCT ON (v.linha) v.*, p.nome AS produto_nome, p.unidade AS produto_unidade
                FROM vinculos_itens_cotacao v JOIN produtos p ON p.id = v.produto_id
                WHERE v.tentativa_id = ? ORDER BY v.linha, v.id DESC
                """, this::mapear, tentativa);
    }

    public long inserir(long tentativa, int linha, long conferencia, Long revisao, long ator,
            long produto, Identidade identidade) {
        return jdbc.queryForObject("""
                INSERT INTO vinculos_itens_cotacao (tentativa_id, linha, conferencia_id, revisao_item_id, ator_id,
                    produto_id, referencia, descricao, unidade, embalagem, fornecedor_id)
                SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, fornecedor_id FROM tentativas_importacao WHERE id = ?
                RETURNING id
                """, Long.class, tentativa, linha, conferencia, revisao, ator, produto, identidade.referencia(),
                identidade.descricao(), identidade.unidade().name(), identidade.embalagem(), tentativa);
    }

    private VinculoItem mapear(ResultSet rs, int linha) throws SQLException {
        long produtoId = rs.getLong("produto_id");
        var produto = new ProdutoCatalogo(produtoId, String.format(Locale.ROOT, "PRD-%06d", produtoId),
                rs.getString("produto_nome"), Unidade.valueOf(rs.getString("produto_unidade")));
        var identidade = new Identidade(rs.getString("referencia"), rs.getString("descricao"),
                Unidade.valueOf(rs.getString("unidade")), rs.getString("embalagem"));
        return new VinculoItem(rs.getLong("id"), rs.getInt("linha"), produto, identidade);
    }
}
