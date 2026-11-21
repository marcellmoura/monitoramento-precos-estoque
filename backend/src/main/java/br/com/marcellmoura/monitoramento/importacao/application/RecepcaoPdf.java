package br.com.marcellmoura.monitoramento.importacao.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import br.com.marcellmoura.monitoramento.fornecedor.persistence.FornecedorRepository;
import br.com.marcellmoura.monitoramento.importacao.domain.EnvioPdf;
import br.com.marcellmoura.monitoramento.importacao.domain.TentativaImportacao;
import br.com.marcellmoura.monitoramento.importacao.domain.TentativaImportacao.Status;

@Service
public class RecepcaoPdf {
    private static final Logger LOG = LoggerFactory.getLogger(RecepcaoPdf.class);
    private final FornecedorRepository fornecedores;
    private final RegistroImportacoes registro;
    private final ValidadorPdf validador;
    private final ArmazenamentoPdf armazenamento;
    private final long limiteBytes;

    RecepcaoPdf(FornecedorRepository fornecedores, RegistroImportacoes registro, ValidadorPdf validador,
            ArmazenamentoPdf armazenamento, @Value("${app.importacao.limite-bytes}") long limiteBytes) {
        if (limiteBytes < 1) throw new IllegalArgumentException("O limite em bytes deve ser positivo.");
        this.fornecedores = fornecedores;
        this.registro = registro;
        this.validador = validador;
        this.armazenamento = armazenamento;
        this.limiteBytes = limiteBytes;
    }

    public TentativaImportacao receber(long fornecedorId, long atorId, String nome, long tamanho, InputStream conteudo) {
        if (!fornecedores.existsById(fornecedorId)) {
            throw new ImportacaoException(404, "FORNECEDOR_NAO_ENCONTRADO", "Fornecedor não encontrado.");
        }
        var envio = new EnvioPdf(fornecedorId, atorId, nomeExibicao(nome));
        var tentativa = processar(envio, tamanho, conteudo);
        LOG.info("Envio PDF {} registrado como {}", tentativa.id(), tentativa.status());
        return tentativa;
    }

    private TentativaImportacao processar(EnvioPdf envio, long tamanho, InputStream conteudo) {
        try {
            if (tamanho == 0) throw new PdfRejeitadoException("Selecione um PDF que não esteja vazio.");
            if (!envio.nomeOriginal().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                throw new PdfRejeitadoException("Escolha um arquivo com extensão .pdf.");
            }
            if (tamanho > limiteBytes) throw new PdfRejeitadoException("O PDF ultrapassa o limite de " + limiteBytes + " bytes.");
            try (var temporario = armazenamento.copiar(conteudo)) {
                int paginas = validador.validar(temporario.caminho());
                return registro.registrarArquivo(envio, temporario, paginas);
            }
        } catch (PdfRejeitadoException ex) {
            return registro.registrarFalha(envio, Status.REJEITADA, ex.getMessage());
        } catch (IOException ex) {
            LOG.warn("Falha de armazenamento no envio PDF: {}", ex.getClass().getSimpleName());
            return registro.registrarFalha(envio, Status.FALHA_ENVIO, "Não foi possível guardar o PDF. Tente novamente.");
        }
    }

    private String nomeExibicao(String nome) {
        if (nome == null) return "Sem arquivo";
        String caminho = nome.replace('\\', '/');
        String arquivo = caminho.substring(caminho.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").strip();
        if (arquivo.isEmpty()) return "Sem arquivo";
        int limite = Math.min(255, arquivo.codePointCount(0, arquivo.length()));
        return arquivo.substring(0, arquivo.offsetByCodePoints(0, limite));
    }
}
