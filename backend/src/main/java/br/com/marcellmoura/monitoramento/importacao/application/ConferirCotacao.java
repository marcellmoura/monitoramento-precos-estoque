package br.com.marcellmoura.monitoramento.importacao.application;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.importacao.domain.ConferenciaCotacao;
import br.com.marcellmoura.monitoramento.importacao.domain.ConferenciaCotacao.Base;
import br.com.marcellmoura.monitoramento.importacao.domain.ConferenciaCotacao.Revisao;
import br.com.marcellmoura.monitoramento.importacao.domain.PrecoCotado;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao.Item;
import br.com.marcellmoura.monitoramento.importacao.persistence.ConferenciaRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.PrecoConfirmadoRepository;

@Service
@Transactional(readOnly = true)
public class ConferirCotacao {
    private static final DateTimeFormatter DATA_PDF = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    private final RegistroImportacoes importacoes;
    private final PrecoConfirmadoRepository precos;
    private final ConferenciaRepository conferencias;
    private final ZoneId fuso;

    public ConferirCotacao(RegistroImportacoes importacoes, ConferenciaRepository conferencias,
            PrecoConfirmadoRepository precos, @Value("${app.importacao.fuso-comercial}") String fuso) {
        this.importacoes = importacoes;
        this.precos = precos;
        this.conferencias = conferencias;
        this.fuso = ZoneId.of(fuso);
    }

    public record Escolha(long revisao, Base base, LocalDate dataComercial) {}

    public ConferenciaCotacao consultar(long id) {
        var previa = importacoes.previa(id);
        var atual = conferencias.atual(id).orElse(null);
        return montar(id, previa, atual);
    }

    @Transactional
    public ConferenciaCotacao confirmar(long id, long ator, Escolha escolha) {
        conferencias.bloquearTentativa(id);
        var previa = importacoes.previa(id);
        var colunas = colunas(previa);
        validar(previa.layout(), colunas, escolha);
        var atual = conferencias.atual(id).orElse(null);
        if (atual != null && atual.base().equals(escolha.base()) && atual.dataComercial().equals(escolha.dataComercial())) {
            return montar(id, previa, atual);
        }
        if (precos.temConfirmados(id)) {
            throw new ImportacaoException(409, "CONFERENCIA_JA_USADA", "A conferência está bloqueada após a confirmação de preços.");
        }
        if (escolha.revisao() != (atual == null ? 0 : atual.id())) {
            throw new ImportacaoException(409, "CONFERENCIA_DESATUALIZADA", "Outra conferência foi salva. Reabra a página para revisar.");
        }
        var salva = conferencias.salvar(id, ator, previa.layout(), assinatura(colunas), escolha.base(), escolha.dataComercial());
        return montar(id, previa, salva);
    }

    private ConferenciaCotacao montar(long id, PreviaImportacao previa, Revisao atual) {
        var colunas = colunas(previa);
        var sugestao = atual == null ? conferencias.sugestao(id, previa.layout(), assinatura(colunas))
                .filter(base -> colunas.contains(base.coluna()))
                .filter(base -> previa.itens().stream().allMatch(item -> pendencias(item, base).isEmpty()))
                .orElse(null) : null;
        var pendentes = atual == null ? List.<List<String>>of() :
                previa.itens().stream().map(item -> pendencias(item, atual.base())).toList();
        return new ConferenciaCotacao(colunas, dataSugerida(id, previa.dataReferencia()), atual, sugestao, pendentes);
    }

    private void validar(String layout, List<String> colunas, Escolha escolha) {
        if (!colunas.contains(escolha.base().coluna())) {
            throw new ImportacaoException(400, "COLUNA_INDISPONIVEL", "Selecione uma coluna da prévia.");
        }
        if (!ConferenciaCotacao.unidadeCompativel(layout, escolha.base().coluna(), escolha.base().unidade())) {
            throw new ImportacaoException(400, "UNIDADE_INCOMPATIVEL", "A coluna Preço KG exige a unidade KG.");
        }
        if ("Não informada".equalsIgnoreCase(escolha.base().embalagem())) {
            throw new ImportacaoException(400, "EMBALAGEM_INVALIDA", "Informe a embalagem confirmada para a cotação.");
        }
        if (escolha.dataComercial().getYear() < 1 || escolha.dataComercial().getYear() > 9999) {
            throw new ImportacaoException(400, "DATA_COMERCIAL_INVALIDA", "Informe uma data comercial válida.");
        }
    }

    private List<String> colunas(PreviaImportacao previa) {
        return previa.itens().stream().flatMap(item -> item.precos().keySet().stream()).distinct().sorted().toList();
    }

    private String assinatura(List<String> colunas) { return String.join("\n", colunas); }

    private LocalDate dataSugerida(long id, String dataPdf) {
        var extraida = interpretarData(dataPdf);
        return extraida != null ? extraida : importacoes.consultar(id).enviadaEm().atZone(fuso).toLocalDate();
    }

    private LocalDate interpretarData(String texto) {
        if (texto == null) return null;
        try {
            var data = LocalDate.parse(texto, DATA_PDF);
            return data.getYear() > 0 ? data : null;
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private List<String> pendencias(Item item, Base base) {
        if (item.dados() == null) return List.of("Revise a linha antes de usar a base comercial.");
        var avisos = new ArrayList<String>();
        if (!base.unidade().name().equalsIgnoreCase(item.dados().unidade())) {
            avisos.add("Confirme a unidade deste item antes de usar a base comercial.");
        }
        if (!base.embalagem().equalsIgnoreCase(item.dados().apresentacao())) {
            avisos.add("Confirme a embalagem deste item antes de usar a base comercial.");
        }
        if (!PrecoCotado.positivo(item.precos().get(base.coluna()))) {
            avisos.add("O preço na coluna escolhida está ausente ou inválido.");
        }
        return avisos;
    }
}
