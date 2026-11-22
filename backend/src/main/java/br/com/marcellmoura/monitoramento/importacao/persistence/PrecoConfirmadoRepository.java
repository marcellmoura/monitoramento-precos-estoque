package br.com.marcellmoura.monitoramento.importacao.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.Confirmado;
import br.com.marcellmoura.monitoramento.importacao.domain.TentativaImportacao.Status;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.Identidade;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

@Repository
public class PrecoConfirmadoRepository {
    private final JdbcTemplate jdbc;

    public PrecoConfirmadoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Chave(String item, Unidade unidade, String embalagem, String modalidade) {
        public static Chave de(Identidade identidade, long produto, String modalidade) {
            String referencia = identidade.referencia();
            return new Chave(referencia == null ? "P:" + produto : "R:" + referencia, identidade.unidade(),
                    identidade.embalagem().strip().toLowerCase(Locale.ROOT), modalidade);
        }
    }

    public record NovoPreco(int linha, long conferencia, Long revisao, long vinculo, long produto, Identidade identidade,
            Chave chave, LocalDate data, BigDecimal preco) {}

    public void bloquearFornecedor(long tentativa) {
        jdbc.query("""
                SELECT f.id FROM fornecedores f JOIN tentativas_importacao t ON t.fornecedor_id = f.id
                WHERE t.id = ? FOR NO KEY UPDATE OF f
                """, rs -> {}, tentativa);
    }

    public Map<Integer, Confirmado> listar(long tentativa) {
        return jdbc.query("SELECT * FROM precos_confirmados WHERE tentativa_id = ?", this::mapear, tentativa)
                .stream().collect(Collectors.toMap(Confirmado::linha, Function.identity()));
    }

    public boolean temConfirmados(long tentativa) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM precos_confirmados WHERE tentativa_id = ?)
                """, Boolean.class, tentativa));
    }

    public boolean confirmado(long tentativa, int linha) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM precos_confirmados WHERE tentativa_id = ? AND linha = ?)
                """, Boolean.class, tentativa, linha));
    }

    public Set<Chave> ocupadas(long tentativa, LocalDate data) {
        return Set.copyOf(jdbc.query("""
                SELECT chave_item, unidade, embalagem_chave, modalidade FROM precos_confirmados
                WHERE fornecedor_id = (SELECT fornecedor_id FROM tentativas_importacao WHERE id = ?)
                  AND data_comercial = ?
                """, (rs, linha) -> new Chave(rs.getString("chave_item"), Unidade.valueOf(rs.getString("unidade")),
                        rs.getString("embalagem_chave"), rs.getString("modalidade")), tentativa, data));
    }

    public Confirmado inserir(long tentativa, long ator, NovoPreco reg) {
        var item = reg.identidade();
        return jdbc.queryForObject("""
                INSERT INTO precos_confirmados (tentativa_id, linha, fornecedor_id, produto_id, conferencia_id,
                    revisao_item_id, vinculo_origem_id, ator_id, chave_item, referencia, descricao, unidade,
                    embalagem, embalagem_chave, modalidade, preco, data_comercial, enviada_em)
                SELECT t.id, ?, t.fornecedor_id, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, t.enviada_em
                FROM tentativas_importacao t WHERE t.id = ? RETURNING *
                """, this::mapear, reg.linha(), reg.produto(), reg.conferencia(), reg.revisao(), reg.vinculo(), ator,
                reg.chave().item(), item.referencia(), item.descricao(), item.unidade().name(), item.embalagem(),
                reg.chave().embalagem(), reg.chave().modalidade(), reg.preco(), reg.data(), tentativa);
    }

    public void atualizarEstado(long tentativa, Status estado) {
        jdbc.update("UPDATE tentativas_importacao SET status = ? WHERE id = ?", estado.name(), tentativa);
    }

    private Confirmado mapear(ResultSet rs, int posicao) throws SQLException {
        return new Confirmado(rs.getLong("id"), rs.getInt("linha"), rs.getLong("produto_id"), rs.getLong("vinculo_origem_id"),
                rs.getBigDecimal("preco"), rs.getObject("data_comercial", LocalDate.class),
                rs.getObject("confirmada_em", OffsetDateTime.class).toInstant());
    }
}
