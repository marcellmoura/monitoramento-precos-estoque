package br.com.marcellmoura.monitoramento.importacao.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.Confirmado;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.PrecoAnterior;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.Resolucao;
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

    public Map<Chave, List<PrecoAnterior>> vigentes(long tentativa, LocalDate data) {
        return jdbc.query("""
                SELECT chave_item, unidade, embalagem_chave, modalidade, id, tentativa_id, preco, ordem
                FROM precos_confirmados
                WHERE fornecedor_id = (SELECT fornecedor_id FROM tentativas_importacao WHERE id = ?)
                  AND data_comercial = ? AND ordem IS NOT NULL
                ORDER BY ordem
                """, (rs, linha) -> Map.entry(new Chave(rs.getString("chave_item"), Unidade.valueOf(rs.getString("unidade")),
                        rs.getString("embalagem_chave"), rs.getString("modalidade")),
                        new PrecoAnterior(rs.getLong("id"), rs.getLong("tentativa_id"),
                                rs.getBigDecimal("preco"), rs.getInt("ordem"))), tentativa, data).stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    public boolean resolucaoRegistrada(long id, Resolucao resolucao) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM precos_confirmados
                    WHERE id = ? AND resolucao = ? AND preco_referencia_id = ?
                      AND motivo_resolucao IS NOT DISTINCT FROM ? AND revisao_conflito = ?)
                """, Boolean.class, id, resolucao.operacao().name(), resolucao.precoReferencia(),
                resolucao.motivo(), resolucao.revisao()));
    }

    public void liberarOrdem(long id) {
        jdbc.update("UPDATE precos_confirmados SET ordem = NULL WHERE id = ?", id);
    }

    public void abrirPosicao(long tentativa, NovoPreco novo, int ordem) {
        var chave = novo.chave();
        jdbc.update("""
                UPDATE precos_confirmados SET ordem = ordem + 1
                WHERE fornecedor_id = (SELECT fornecedor_id FROM tentativas_importacao WHERE id = ?)
                  AND data_comercial = ? AND chave_item = ? AND unidade = ?
                  AND embalagem_chave = ? AND modalidade = ? AND ordem >= ?
                """, tentativa, novo.data(), chave.item(), chave.unidade().name(), chave.embalagem(), chave.modalidade(), ordem);
    }

    public Confirmado inserir(long tentativa, long ator, NovoPreco reg, int ordem, Resolucao resolucao) {
        var item = reg.identidade();
        return jdbc.queryForObject("""
                INSERT INTO precos_confirmados (tentativa_id, linha, fornecedor_id, produto_id, conferencia_id,
                    revisao_item_id, vinculo_origem_id, ator_id, chave_item, referencia, descricao, unidade,
                    embalagem, embalagem_chave, modalidade, preco, data_comercial, enviada_em, ordem,
                    resolucao, preco_referencia_id, motivo_resolucao, revisao_conflito)
                SELECT t.id, ?, t.fornecedor_id, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, t.enviada_em, ?, ?, ?, ?, ?
                FROM tentativas_importacao t WHERE t.id = ? RETURNING *
                """, this::mapear, reg.linha(), reg.produto(), reg.conferencia(), reg.revisao(), reg.vinculo(), ator,
                reg.chave().item(), item.referencia(), item.descricao(), item.unidade().name(), item.embalagem(),
                reg.chave().embalagem(), reg.chave().modalidade(), reg.preco(), reg.data(), ordem,
                resolucao == null ? null : resolucao.operacao().name(),
                resolucao == null ? null : resolucao.precoReferencia(),
                resolucao == null ? null : resolucao.motivo(),
                resolucao == null ? null : resolucao.revisao(), tentativa);
    }

    public void atualizarEstado(long tentativa, Status estado) {
        jdbc.update("UPDATE tentativas_importacao SET status = ? WHERE id = ?", estado.name(), tentativa);
    }

    private Confirmado mapear(ResultSet rs, int posicao) throws SQLException {
        return new Confirmado(rs.getLong("id"), rs.getInt("linha"), rs.getLong("produto_id"), rs.getLong("vinculo_origem_id"),
                rs.getBigDecimal("preco"), rs.getObject("data_comercial", LocalDate.class),
                rs.getObject("confirmada_em", OffsetDateTime.class).toInstant(), rs.getObject("ordem") == null);
    }
}
