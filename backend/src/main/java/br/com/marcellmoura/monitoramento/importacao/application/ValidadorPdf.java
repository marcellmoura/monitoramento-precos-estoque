package br.com.marcellmoura.monitoramento.importacao.application;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class ValidadorPdf {
    private final int limitePaginas;

    ValidadorPdf(@Value("${app.importacao.limite-paginas}") int limitePaginas) {
        if (limitePaginas < 1) throw new IllegalArgumentException("O limite de páginas deve ser positivo.");
        this.limitePaginas = limitePaginas;
    }

    int validar(Path caminho) throws PdfRejeitadoException {
        try (var entrada = new RandomAccessReadBufferedFile(caminho);
                var documento = new PDFParser(entrada, "", null, null, IOUtils.createTempFileOnlyStreamCache()).parse(false)) {
            if (documento.isEncrypted()) throw pdfProtegido();
            int paginas = documento.getNumberOfPages();
            if (paginas < 1 || paginas > limitePaginas) {
                throw new PdfRejeitadoException("O PDF deve ter entre 1 e " + limitePaginas + " páginas.");
            }
            var sonda = new PresencaDeTexto();
            sonda.writeText(documento, Writer.nullWriter());
            if (!sonda.encontrouTexto) {
                throw new PdfRejeitadoException("O PDF não contém texto legível. Envie um PDF textual do fornecedor.");
            }
            return paginas;
        } catch (InvalidPasswordException ex) {
            throw pdfProtegido();
        } catch (IOException ex) {
            throw new PdfRejeitadoException("O arquivo não é um PDF válido ou está corrompido.");
        }
    }

    private PdfRejeitadoException pdfProtegido() {
        return new PdfRejeitadoException("O PDF está protegido. Envie uma cópia sem proteção.");
    }

    private static final class PresencaDeTexto extends PDFTextStripper {
        private boolean encontrouTexto;

        @Override public void processPage(PDPage pagina) throws IOException {
            if (!encontrouTexto) super.processPage(pagina);
        }

        @Override protected void processTextPosition(TextPosition texto) {
            if (!encontrouTexto && texto.getUnicode() != null) {
                encontrouTexto = texto.getUnicode().codePoints().anyMatch(Character::isLetterOrDigit);
            }
        }
    }
}
