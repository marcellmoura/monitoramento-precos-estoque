package br.com.marcellmoura.monitoramento.importacao.extracao;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao;

@Component
public class LeitorCotacaoPdf {
    public PreviaImportacao extrair(Path caminho) throws IOException, ExtracaoPdfException {
        try (var pdf = Loader.loadPDF(caminho.toFile(), IOUtils.createTempFileOnlyStreamCache())) {
            var leitor = new PDFTextStripper();
            leitor.setSortByPosition(true);
            leitor.setWordSeparator("\t");
            leitor.setLineSeparator("\n");
            var texto = new TextoLimitado();
            leitor.writeText(pdf, texto);
            return new InterpretadorCotacao().interpretar(texto.conteudo.toString());
        }
    }

    private static final class TextoLimitado extends Writer {
        private static final int LIMITE = 500_000;
        private final StringBuilder conteudo = new StringBuilder();

        @Override public void write(char[] texto, int inicio, int tamanho) throws IOException {
            if (tamanho > LIMITE - conteudo.length()) throw new IOException("Limite de texto atingido.");
            conteudo.append(texto, inicio, tamanho);
        }

        @Override public void flush() {}
        @Override public void close() {}
    }
}
