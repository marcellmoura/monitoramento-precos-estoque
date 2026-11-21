package br.com.marcellmoura.monitoramento.importacao.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import br.com.marcellmoura.monitoramento.importacao.domain.ConferenciaCotacao.Base;
import br.com.marcellmoura.monitoramento.importacao.domain.ConferenciaCotacao.Revisao;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

@Repository
public class ConferenciaRepository {
    private final JdbcTemplate jdbc;
    public ConferenciaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void bloquearTentativa(long id) {
        jdbc.query("SELECT id FROM tentativas_importacao WHERE id = ? FOR UPDATE", rs -> {}, id);
    }

    public Optional<Revisao> atual(long id) {
        return jdbc.query("""
                SELECT id, coluna, unidade, embalagem, data_comercial FROM conferencias_cotacao
                WHERE tentativa_id = ? ORDER BY id DESC LIMIT 1
                """, this::revisao, id).stream().findFirst();
    }

    private record Configuracao(String colunas, Base base) {}

    public Optional<Base> sugestao(long tentativa, String layout, String colunas) {
        return jdbc.query("""
                SELECT c.coluna, c.unidade, c.embalagem, c.colunas FROM conferencias_cotacao c
                JOIN tentativas_importacao t ON t.id = c.tentativa_id
                WHERE t.fornecedor_id = (SELECT fornecedor_id FROM tentativas_importacao WHERE id = ?)
                  AND c.layout = ? ORDER BY c.id DESC LIMIT 1
                """, (rs, linha) -> new Configuracao(rs.getString("colunas"), base(rs)), tentativa, layout)
                .stream().findFirst().filter(config -> colunas.equals(config.colunas())).map(Configuracao::base);
    }

    public Revisao salvar(long tentativa, long ator, String layout, String colunas, Base base, LocalDate data) {
        long id = jdbc.queryForObject("""
                INSERT INTO conferencias_cotacao (tentativa_id, ator_id, layout, colunas, coluna, unidade, embalagem, data_comercial)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, tentativa, ator, layout, colunas, base.coluna(), base.unidade().name(), base.embalagem(), data);
        return new Revisao(id, base, data);
    }

    private Revisao revisao(ResultSet dados, int linha) throws SQLException {
        return new Revisao(dados.getLong("id"), base(dados), dados.getObject("data_comercial", LocalDate.class));
    }

    private Base base(ResultSet dados) throws SQLException {
        return new Base(dados.getString("coluna"), Unidade.valueOf(dados.getString("unidade")), dados.getString("embalagem"));
    }
}
