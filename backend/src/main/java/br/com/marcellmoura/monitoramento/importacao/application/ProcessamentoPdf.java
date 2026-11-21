package br.com.marcellmoura.monitoramento.importacao.application;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao;
import br.com.marcellmoura.monitoramento.importacao.extracao.ExtracaoPdfException;
import br.com.marcellmoura.monitoramento.importacao.extracao.LeitorCotacaoPdf;
import br.com.marcellmoura.monitoramento.importacao.persistence.ExtracaoRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.ExtracaoRepository.Trabalho;

@Service
public class ProcessamentoPdf {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessamentoPdf.class);
    private final ExtracaoRepository extracoes;
    private final LeitorCotacaoPdf leitor;
    private final ArmazenamentoPdf armazenamento;
    private final long prazo;

    ProcessamentoPdf(ExtracaoRepository extracoes, LeitorCotacaoPdf leitor, ArmazenamentoPdf armazenamento,
            @Value("${app.importacao.prazo-extracao-segundos}") long prazo) {
        if (prazo < 1) throw new IllegalArgumentException("O prazo de extração deve ser positivo.");
        this.extracoes = extracoes;
        this.leitor = leitor;
        this.armazenamento = armazenamento;
        this.prazo = prazo;
    }

    public void executarFila() {
        int expiradas = extracoes.expirar();
        if (expiradas > 0) LOG.warn("{} extrações expiradas foram marcadas como falha.", expiradas);
        extracoes.reivindicar(prazo).ifPresent(this::processar);
    }

    private void processar(Trabalho trabalho) {
        LOG.info("Início da extração {} para a tentativa {}", trabalho.execucao(), trabalho.tentativa());
        PreviaImportacao previa;
        try {
            previa = leitor.extrair(armazenamento.caminhoOriginal(trabalho.arquivo()));
        } catch (ExtracaoPdfException ex) {
            registrarFalha(trabalho, ex.getMessage());
            return;
        } catch (IOException | RuntimeException ex) {
            LOG.warn("Falha de leitura da extração {}: {}", trabalho.execucao(), ex.getClass().getSimpleName());
            registrarFalha(trabalho, "Não foi possível ler o PDF armazenado.");
            return;
        }
        if (extracoes.concluir(trabalho, previa)) LOG.info("Extração {} concluída.", trabalho.execucao());
    }

    private void registrarFalha(Trabalho trabalho, String motivo) {
        if (extracoes.falhar(trabalho, motivo)) LOG.warn("Extração {} encerrada com falha.", trabalho.execucao());
    }
}
