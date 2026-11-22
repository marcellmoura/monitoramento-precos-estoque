package br.com.marcellmoura.monitoramento.importacao.application;

import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.importacao.domain.HistoricoPrecos;
import br.com.marcellmoura.monitoramento.importacao.domain.HistoricoPrecos.Filtros;
import br.com.marcellmoura.monitoramento.importacao.persistence.HistoricoPrecoRepository;

@Service
public class ConsultarHistoricoPrecos {
    private final HistoricoPrecoRepository precos;

    public ConsultarHistoricoPrecos(HistoricoPrecoRepository precos) {
        this.precos = precos;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HistoricoPrecos consultar(Filtros filtros, int pagina, int tamanho) {
        var inicio = filtros.inicio();
        var fim = filtros.fim();
        if (foraDaFaixa(inicio) || foraDaFaixa(fim) || (inicio != null && fim != null && inicio.isAfter(fim))) {
            throw new ImportacaoException(400, "PERIODO_INVALIDO", "Informe um período comercial válido com início até o fim.");
        }
        var resultado = precos.listar(filtros, PageRequest.of(pagina, tamanho));
        return new HistoricoPrecos(resultado.getContent(), resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    private static boolean foraDaFaixa(LocalDate data) {
        return data != null && (data.getYear() < 1 || data.getYear() > 9999);
    }
}
