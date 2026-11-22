package br.com.marcellmoura.monitoramento.importacao.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao;

@Repository
public class ExtracaoRepository {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final JdbcTemplate jdbc;

    public ExtracaoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Trabalho(long tentativa, long execucao, UUID arquivo) {}
    private record Pendente(long tentativa, UUID arquivo) {}

    @Transactional
    public Optional<Trabalho> reivindicar(long prazoSegundos) {
        return jdbc.query("""
                SELECT t.id, a.arquivo_id FROM tentativas_importacao t
                JOIN arquivos_importacao a ON a.hash = t.arquivo_hash
                WHERE t.status = 'PENDENTE' ORDER BY t.id LIMIT 1 FOR UPDATE OF t SKIP LOCKED
                """, (rs, linha) -> new Pendente(rs.getLong("id"), rs.getObject("arquivo_id", UUID.class)))
                .stream().findFirst().map(pendente -> iniciar(pendente, prazoSegundos));
    }

    private Trabalho iniciar(Pendente pendente, long prazoSegundos) {
        long id = jdbc.queryForObject("""
                INSERT INTO execucoes_extracao (tentativa_id, expira_em)
                VALUES (?, clock_timestamp() + (? * INTERVAL '1 second')) RETURNING id
                """, Long.class, pendente.tentativa(), prazoSegundos);
        jdbc.update("UPDATE tentativas_importacao SET status = 'PROCESSANDO', execucao_atual = ? WHERE id = ?",
                id, pendente.tentativa());
        return new Trabalho(pendente.tentativa(), id, pendente.arquivo());
    }

    @Transactional
    public boolean concluir(Trabalho trabalho, PreviaImportacao previa) {
        return finalizar(trabalho, "AGUARDANDO_CONFERENCIA", null, previa);
    }

    @Transactional
    public boolean falhar(Trabalho trabalho, String motivo) {
        return finalizar(trabalho, "FALHA_EXTRACAO", motivo, null);
    }

    private boolean finalizar(Trabalho trabalho, String status, String motivo, PreviaImportacao previa) {
        int afetadas = jdbc.update("""
                UPDATE tentativas_importacao t SET status = ?, motivo = ?, quantidade_itens = ?
                FROM execucoes_extracao e
                WHERE t.id = ? AND t.status = 'PROCESSANDO' AND t.execucao_atual = ? AND e.id = t.execucao_atual
                  AND e.expira_em > clock_timestamp()
                """, status, motivo, previa == null ? null : previa.itens().size(), trabalho.tentativa(), trabalho.execucao());
        if (afetadas == 0) return false;
        jdbc.update("""
                UPDATE execucoes_extracao SET finalizada_em = clock_timestamp(), motivo = ?, previa = CAST(? AS JSONB) WHERE id = ?
                """, motivo, previa == null ? null : JSON.writeValueAsString(previa), trabalho.execucao());
        return true;
    }

    @Transactional
    public int expirar() {
        String motivo = "O processamento foi interrompido ou ultrapassou o tempo limite. Tente novamente.";
        return jdbc.update("""
                WITH expiradas AS (
                    UPDATE tentativas_importacao t SET status = 'FALHA_EXTRACAO', motivo = ?
                    FROM execucoes_extracao e
                    WHERE t.status = 'PROCESSANDO' AND e.id = t.execucao_atual AND e.expira_em <= clock_timestamp()
                    RETURNING t.execucao_atual
                )
                UPDATE execucoes_extracao e SET finalizada_em = clock_timestamp(), motivo = ?
                FROM expiradas WHERE e.id = expiradas.execucao_atual
                """, motivo, motivo);
    }

    public Optional<PreviaImportacao> previa(long id) {
        return jdbc.queryForList("""
                SELECT e.previa::text FROM tentativas_importacao t JOIN execucoes_extracao e ON e.id = t.execucao_atual
                WHERE t.id = ? AND t.status IN ('AGUARDANDO_CONFERENCIA','CONCLUIDA_COM_PENDENCIAS','CONCLUIDA')
                """, String.class, id).stream().findFirst().map(json -> JSON.readValue(json, PreviaImportacao.class));
    }

    public boolean reagendar(long id) {
        return jdbc.update("""
                UPDATE tentativas_importacao SET status = 'PENDENTE', motivo = NULL, execucao_atual = NULL
                WHERE id = ? AND status = 'FALHA_EXTRACAO'
                """, id) == 1;
    }
}
