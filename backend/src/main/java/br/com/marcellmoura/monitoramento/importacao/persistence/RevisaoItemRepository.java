package br.com.marcellmoura.monitoramento.importacao.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import br.com.marcellmoura.monitoramento.importacao.domain.RevisaoItem;
import br.com.marcellmoura.monitoramento.importacao.domain.RevisaoItem.Dados;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

@Repository
public class RevisaoItemRepository {
    private final JdbcTemplate jdbc;

    public RevisaoItemRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<RevisaoItem> listar(long tentativa) {
        return jdbc.query("""
                SELECT DISTINCT ON (linha) * FROM revisoes_itens_cotacao
                WHERE tentativa_id = ? ORDER BY linha, id DESC
                """, this::revisao, tentativa);
    }

    public Optional<RevisaoItem> atual(long tentativa, int linha) {
        return jdbc.query("""
                SELECT * FROM revisoes_itens_cotacao
                WHERE tentativa_id = ? AND linha = ? ORDER BY id DESC LIMIT 1
                """, this::revisao, tentativa, linha).stream().findFirst();
    }

    public RevisaoItem salvar(long tentativa, int linha, long conferencia, long ator, Dados dados) {
        return jdbc.queryForObject("""
                INSERT INTO revisoes_itens_cotacao
                    (tentativa_id, linha, conferencia_id, ator_id, referencia, descricao,
                     unidade, embalagem, quantidade, coluna, preco, motivo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *
                """, this::revisao, tentativa, linha, conferencia, ator, dados.referencia(), dados.descricao(),
                dados.unidade() == null ? null : dados.unidade().name(), dados.embalagem(),
                dados.quantidade(), dados.coluna(), dados.preco(), dados.motivo());
    }

    private RevisaoItem revisao(ResultSet rs, int numero) throws SQLException {
        var unidade = rs.getString("unidade");
        var dados = new Dados(rs.getString("referencia"), rs.getString("descricao"),
                unidade == null ? null : Unidade.valueOf(unidade), rs.getString("embalagem"),
                rs.getBigDecimal("quantidade"), rs.getString("coluna"), rs.getBigDecimal("preco"), rs.getString("motivo"));
        return new RevisaoItem(rs.getLong("id"), rs.getInt("linha"), rs.getLong("ator_id"),
                rs.getObject("revisada_em", OffsetDateTime.class).toInstant(), dados, RevisaoItem.verificar(dados));
    }
}
