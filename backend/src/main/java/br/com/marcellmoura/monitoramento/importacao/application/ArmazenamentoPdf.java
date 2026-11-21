package br.com.marcellmoura.monitoramento.importacao.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
class ArmazenamentoPdf {
    private static final Logger LOG = LoggerFactory.getLogger(ArmazenamentoPdf.class);
    private final Path pasta;

    ArmazenamentoPdf(@Value("${app.importacao.pasta}") Path pasta) { this.pasta = pasta.toAbsolutePath().normalize(); }

    Temporario copiar(InputStream conteudo) throws IOException {
        Files.createDirectories(pasta);
        Path caminho = Files.createTempFile(pasta, "envio-", ".part");
        try (var leitura = new DigestInputStream(conteudo, sha256())) {
            long tamanho = Files.copy(leitura, caminho, StandardCopyOption.REPLACE_EXISTING);
            return new Temporario(caminho, HexFormat.of().formatHex(leitura.getMessageDigest().digest()), tamanho);
        } catch (IOException | RuntimeException ex) {
            remover(caminho);
            throw ex;
        }
    }

    UUID guardar(Temporario temporario) throws IOException {
        UUID id = UUID.randomUUID();
        Path destino = caminhoOriginal(id);
        Files.move(temporario.caminho(), destino, StandardCopyOption.ATOMIC_MOVE);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) remover(destino);
            }
        });
        return id;
    }

    Path caminhoOriginal(UUID id) { return pasta.resolve(id + ".pdf"); }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível.", ex);
        }
    }

    private static void remover(Path caminho) {
        try {
            Files.deleteIfExists(caminho);
        } catch (IOException ex) {
            LOG.warn("Não foi possível limpar um arquivo temporário de importação: {}", ex.getClass().getSimpleName());
        }
    }

    record Temporario(Path caminho, String hash, long tamanho) implements AutoCloseable {
        @Override public void close() { remover(caminho); }
    }
}
