package br.com.marcellmoura.monitoramento.importacao.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import br.com.marcellmoura.monitoramento.fornecedor.domain.Fornecedor;
import br.com.marcellmoura.monitoramento.importacao.domain.HistoricoPrecos.Filtros;
import br.com.marcellmoura.monitoramento.importacao.domain.HistoricoPrecos.Identificacao;
import br.com.marcellmoura.monitoramento.importacao.domain.HistoricoPrecos.Origem;
import br.com.marcellmoura.monitoramento.importacao.domain.HistoricoPrecos.Registro;
import br.com.marcellmoura.monitoramento.importacao.domain.HistoricoPrecos.VinculoSubstituicao;
import br.com.marcellmoura.monitoramento.produto.domain.Produto;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

@Repository
public class HistoricoPrecoRepository {
    private static final String FILTROS = """
            WHERE (CAST(:produtoId AS bigint) IS NULL OR p.produto_id = :produtoId)
              AND (CAST(:fornecedorId AS bigint) IS NULL OR p.fornecedor_id = :fornecedorId)
              AND (CAST(:tentativa AS bigint) IS NULL OR p.tentativa_id = :tentativa)
              AND (CAST(:inicio AS date) IS NULL OR p.data_comercial >= :inicio)
              AND (CAST(:fim AS date) IS NULL OR p.data_comercial <= :fim)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public HistoricoPrecoRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Page<Registro> listar(Filtros filtros, Pageable pagina) {
        var parametros = new MapSqlParameterSource()
                .addValue("produtoId", filtros.produtoId())
                .addValue("fornecedorId", filtros.fornecedorId())
                .addValue("tentativa", filtros.tentativa())
                .addValue("inicio", filtros.inicio())
                .addValue("fim", filtros.fim())
                .addValue("limite", pagina.getPageSize())
                .addValue("deslocamento", pagina.getOffset());

        Long total = jdbc.queryForObject("SELECT count(*) FROM precos_confirmados p " + FILTROS, parametros, Long.class);
        var itens = jdbc.query("""
                SELECT p.id, p.produto_id, produto.nome AS produto_nome, p.fornecedor_id, fornecedor.nome AS fornecedor_nome,
                       p.unidade, p.embalagem, p.modalidade, p.preco, p.data_comercial, p.ordem,
                       p.tentativa_id, p.linha, t.nome_original,
                       original.tentativa_id AS original_tentativa, original.linha AS original_linha,
                       original.preco AS original_preco, p.motivo_resolucao AS original_motivo,
                       sucessor.tentativa_id AS sucessor_tentativa, sucessor.linha AS sucessor_linha,
                       sucessor.preco AS sucessor_preco, sucessor.motivo_resolucao AS sucessor_motivo
                FROM precos_confirmados p
                JOIN produtos produto ON produto.id = p.produto_id
                JOIN fornecedores fornecedor ON fornecedor.id = p.fornecedor_id
                JOIN tentativas_importacao t ON t.id = p.tentativa_id
                LEFT JOIN precos_confirmados original ON p.resolucao = 'SUBSTITUIR' AND original.id = p.preco_referencia_id
                LEFT JOIN precos_confirmados sucessor ON sucessor.resolucao = 'SUBSTITUIR' AND sucessor.preco_referencia_id = p.id
                """ + FILTROS + """
                ORDER BY p.data_comercial DESC, p.fornecedor_id, p.chave_item, p.unidade, p.embalagem_chave, p.modalidade,
                         p.ordem DESC NULLS LAST, p.id DESC
                LIMIT :limite OFFSET :deslocamento
                """, parametros, this::mapear);
        return new PageImpl<>(itens, pagina, total);
    }

    private Registro mapear(ResultSet rs, int posicao) throws SQLException {
        return new Registro(rs.getLong("id"),
                new Identificacao(Produto.gerarCodigo(rs.getLong("produto_id")), rs.getString("produto_nome")),
                new Identificacao(Fornecedor.gerarCodigo(rs.getLong("fornecedor_id")), rs.getString("fornecedor_nome")),
                Unidade.valueOf(rs.getString("unidade")), rs.getString("embalagem"), rs.getString("modalidade"), rs.getBigDecimal("preco"),
                rs.getObject("data_comercial", LocalDate.class), rs.getObject("ordem", Integer.class),
                new Origem(rs.getLong("tentativa_id"), rs.getInt("linha"), rs.getString("nome_original")),
                mapearVinculo(rs, "original"), mapearVinculo(rs, "sucessor"));
    }

    private VinculoSubstituicao mapearVinculo(ResultSet rs, String prefixo) throws SQLException {
        var tentativa = rs.getObject(prefixo + "_tentativa", Long.class);
        if (tentativa == null) return null;
        return new VinculoSubstituicao(tentativa, rs.getInt(prefixo + "_linha"), rs.getBigDecimal(prefixo + "_preco"),
                rs.getString(prefixo + "_motivo"));
    }
}
