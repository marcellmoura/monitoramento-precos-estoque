package br.com.marcellmoura.monitoramento.importacao.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "app.importacao.processamento-ativo", havingValue = "true", matchIfMissing = true)
class AgendamentoExtracao {
    private final ProcessamentoPdf processamento;
    AgendamentoExtracao(ProcessamentoPdf processamento) { this.processamento = processamento; }

    @Scheduled(fixedDelayString = "${app.importacao.intervalo-extracao-ms}", initialDelayString = "${app.importacao.intervalo-extracao-ms}")
    void processar() { processamento.executarFila(); }
}
