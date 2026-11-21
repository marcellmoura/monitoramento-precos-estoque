package br.com.marcellmoura.monitoramento.importacao.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import br.com.marcellmoura.monitoramento.fornecedor.domain.Fornecedor;
import br.com.marcellmoura.monitoramento.importacao.domain.EnvioPdf;
import br.com.marcellmoura.monitoramento.importacao.domain.TentativaImportacao;
import br.com.marcellmoura.monitoramento.importacao.domain.TentativaImportacao.Status;

@Repository
public class ImportacaoRepository {
    private static final String CONSULTA = """
            SELECT t.id, t.fornecedor_id, f.nome AS fornecedor_nome, t.ator_id, t.nome_original, t.enviada_em,
                   t.status, t.motivo, a.paginas, t.quantidade_itens, t.importacao_existente_id
            FROM tentativas_importacao t
            JOIN fornecedores f ON f.id = t.fornecedor_id
            LEFT JOIN arquivos_importacao a ON a.hash = t.arquivo_hash
            """;
    private final JdbcTemplate jdbc;

    public ImportacaoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void bloquearConteudo(String hash) {
        long chave = Long.parseUnsignedLong(hash.substring(0, 16), 16);
        jdbc.query("SELECT pg_advisory_xact_lock(?)", rs -> {}, chave);
    }

    public boolean arquivoRegistrado(String hash) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM arquivos_importacao WHERE hash = ?)", Boolean.class, hash));
    }

    public Optional<Long> existente(long fornecedorId, String hash) {
        return jdbc.queryForList("""
                SELECT id FROM tentativas_importacao WHERE fornecedor_id = ? AND arquivo_hash = ?
                    AND status NOT IN ('DUPLICADA', 'REJEITADA', 'FALHA_ENVIO')
                """, Long.class, fornecedorId, hash).stream().findFirst();
    }

    public void inserirArquivo(String hash, UUID id, long bytes, int paginas) {
        jdbc.update("INSERT INTO arquivos_importacao (hash, arquivo_id, tamanho_bytes, paginas) VALUES (?, ?, ?, ?)",
                hash, id, bytes, paginas);
    }

    public long inserir(EnvioPdf envio, String hash, Status status, String motivo, Long existente) {
        return jdbc.queryForObject("""
                INSERT INTO tentativas_importacao (fornecedor_id, ator_id, nome_original, arquivo_hash, status, motivo,
                    importacao_existente_id) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, envio.fornecedorId(), envio.atorId(), envio.nomeOriginal(), hash, status.name(), motivo,
                existente);
    }

    public Optional<TentativaImportacao> consultar(long id) {
        return jdbc.query(CONSULTA + " WHERE t.id = ?", this::mapear, id).stream().findFirst();
    }

    public Page<TentativaImportacao> listar(int pagina, int tamanho) {
        var paginacao = PageRequest.of(pagina, tamanho);
        long total = jdbc.queryForObject("SELECT count(*) FROM tentativas_importacao", Long.class);
        var itens = jdbc.query(CONSULTA + " ORDER BY t.id DESC LIMIT ? OFFSET ?", this::mapear, tamanho, paginacao.getOffset());
        return new PageImpl<>(itens, paginacao, total);
    }

    public boolean confirmar(long id, long atorId) {
        return jdbc.update("""
                UPDATE tentativas_importacao SET status = 'PENDENTE', motivo = NULL,
                    confirmada_por = ?, confirmada_em = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'AGUARDANDO_CONFIRMACAO'
                """, atorId, id) == 1;
    }

    private TentativaImportacao mapear(ResultSet dados, int linha) throws SQLException {
        return new TentativaImportacao(dados.getLong("id"), Fornecedor.gerarCodigo(dados.getLong("fornecedor_id")),
                dados.getString("fornecedor_nome"), dados.getLong("ator_id"), dados.getString("nome_original"),
                dados.getTimestamp("enviada_em").toInstant(), Status.valueOf(dados.getString("status")), dados.getString("motivo"),
                dados.getObject("paginas", Integer.class), dados.getObject("quantidade_itens", Integer.class),
                dados.getObject("importacao_existente_id", Long.class));
    }
}
