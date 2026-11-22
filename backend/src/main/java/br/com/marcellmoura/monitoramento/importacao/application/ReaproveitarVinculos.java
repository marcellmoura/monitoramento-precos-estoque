package br.com.marcellmoura.monitoramento.importacao.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import br.com.marcellmoura.monitoramento.importacao.application.AssociarItemCotacao.Linha;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.Reaproveitamento;
import br.com.marcellmoura.monitoramento.importacao.persistence.ConferenciaRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.ExtracaoRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.RevisaoItemRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.VinculoItemRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.VinculoItemRepository.Origem;

@Service
class ReaproveitarVinculos {
    private final ExtracaoRepository extracoes;
    private final ConferenciaRepository conferencias;
    private final RevisaoItemRepository revisoes;
    private final VinculoItemRepository vinculos;

    ReaproveitarVinculos(ExtracaoRepository extracoes, ConferenciaRepository conferencias,
            RevisaoItemRepository revisoes, VinculoItemRepository vinculos) {
        this.extracoes = extracoes;
        this.conferencias = conferencias;
        this.revisoes = revisoes;
        this.vinculos = vinculos;
    }

    Map<Integer, Reaproveitamento> resolver(long tentativa, List<Linha> linhas, Set<Integer> vinculadas) {
        var porReferencia = linhas.stream().filter(Linha::reutilizavel)
                .collect(Collectors.groupingBy(linha -> linha.identidade().referencia()));
        var elegiveis = porReferencia.values().stream().flatMap(List::stream)
                .filter(linha -> !vinculadas.contains(linha.numero()))
                .filter(linha -> identificacaoConsistente(linha, porReferencia.get(linha.identidade().referencia())))
                .toList();
        if (elegiveis.isEmpty()) return Map.of();
        var referencias = elegiveis.stream().map(linha -> linha.identidade().referencia()).collect(Collectors.toSet());
        var porTentativa = new HashMap<Long, List<Linha>>();
        var origens = vinculos.origens(tentativa, referencias).stream()
                .filter(origem -> vigente(origem, porTentativa))
                .collect(Collectors.groupingBy(origem -> origem.vinculo().identidade().referencia()));
        var resultado = new HashMap<Integer, Reaproveitamento>();
        for (var linha : elegiveis) {
            var origem = origemSegura(linha, origens.getOrDefault(linha.identidade().referencia(), List.of()));
            if (origem != null) {
                resultado.put(linha.numero(), new Reaproveitamento(origem.tentativa(), origem.vinculo().id(), origem.vinculo().produto()));
            }
        }
        return resultado;
    }

    private boolean identificacaoConsistente(Linha linha, List<Linha> mesmaReferencia) {
        return mesmaReferencia.stream().filter(outra -> linha.modalidade().equals(outra.modalidade())
                && linha.identidade().mesmaApresentacao(outra.identidade()))
                .allMatch(outra -> linha.identidade().mesmaIdentificacao(outra.identidade()));
    }

    private boolean vigente(Origem origem, Map<Long, List<Linha>> porTentativa) {
        var gravado = origem.vinculo();
        var linhas = porTentativa.computeIfAbsent(origem.tentativa(), this::linhasDaOrigem);
        if (gravado.linha() > linhas.size()) return false;
        var atual = linhas.get(gravado.linha() - 1);
        return atual.reutilizavel() && atual.modalidade().equals(origem.modalidade())
                && atual.identidade().mesmaIdentificacao(gravado.identidade());
    }

    private List<Linha> linhasDaOrigem(long tentativa) {
        return extracoes.previa(tentativa).map(previa -> AssociarItemCotacao.linhas(previa,
                conferencias.atual(tentativa).orElse(null), AssociarItemCotacao.porLinha(revisoes.listar(tentativa))))
                .orElseGet(List::of);
    }

    private Origem origemSegura(Linha linha, List<Origem> historico) {
        var apresentacao = historico.stream().filter(origem -> linha.modalidade().equals(origem.modalidade())
                && linha.identidade().mesmaApresentacao(origem.vinculo().identidade())).toList();
        if (apresentacao.isEmpty()) return null;
        var recente = apresentacao.getFirst();
        var produto = recente.vinculo().produto();
        if (!recente.ativo() || produto.unidade() != linha.identidade().unidade()) return null;
        return apresentacao.stream().allMatch(origem -> origem.vinculo().produto().id() == produto.id()
                && linha.identidade().mesmaIdentificacao(origem.vinculo().identidade())) ? recente : null;
    }
}
