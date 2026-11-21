package br.com.marcellmoura.monitoramento.importacao.application;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.importacao.domain.ConferenciaCotacao;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao;
import br.com.marcellmoura.monitoramento.importacao.domain.RevisaoItem;
import br.com.marcellmoura.monitoramento.importacao.domain.RevisaoItem.Dados;
import br.com.marcellmoura.monitoramento.importacao.persistence.ConferenciaRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.RevisaoItemRepository;

@Service
@Transactional(readOnly = true)
public class RevisarItemCotacao {
    private final RegistroImportacoes importacoes;
    private final ConferenciaRepository conferencias;
    private final RevisaoItemRepository itens;

    public RevisarItemCotacao(RegistroImportacoes importacoes, ConferenciaRepository conferencias, RevisaoItemRepository itens) {
        this.importacoes = importacoes;
        this.conferencias = conferencias;
        this.itens = itens;
    }

    public List<RevisaoItem> listar(long tentativa) {
        importacoes.previa(tentativa);
        return itens.listar(tentativa);
    }

    public record Correcao(long revisao, long conferencia, Dados dados) {}

    @Transactional
    public RevisaoItem salvar(long tentativa, int linha, long ator, Correcao correcao) {
        conferencias.bloquearTentativa(tentativa);
        var previa = importacoes.previa(tentativa);
        if (linha < 1 || linha > previa.itens().size()) {
            throw new ImportacaoException(404, "ITEM_NAO_ENCONTRADO", "Item da prévia não encontrado.");
        }
        var base = conferencias.atual(tentativa).orElseThrow(() ->
                new ImportacaoException(409, "CONFERENCIA_OBRIGATORIA", "Salve a conferência comercial antes de corrigir um item."));
        if (base.id() != correcao.conferencia()) throw desatualizada();

        var dados = correcao.dados();
        validarOrigem(previa, dados);
        var atual = itens.atual(tentativa, linha).orElse(null);
        if (atual != null && atual.dados().equals(dados)) return atual;
        if (correcao.revisao() != (atual == null ? 0 : atual.id())) throw desatualizada();
        return itens.salvar(tentativa, linha, base.id(), ator, dados);
    }

    private ImportacaoException desatualizada() {
        return new ImportacaoException(409, "ITEM_DESATUALIZADO", "Os dados mudaram. Reabra o formulário antes de salvar.");
    }

    private void validarOrigem(PreviaImportacao previa, Dados dados) {
        if (dados.coluna() == null) return;
        if (previa.itens().stream().noneMatch(item -> item.precos().containsKey(dados.coluna()))) {
            throw new ImportacaoException(400, "COLUNA_INDISPONIVEL", "Selecione uma coluna da prévia ou informe o valor conferido externamente.");
        }
        if (!ConferenciaCotacao.unidadeCompativel(previa.layout(), dados.coluna(), dados.unidade())) {
            throw new ImportacaoException(400, "UNIDADE_INCOMPATIVEL", "A coluna Preço KG exige a unidade KG.");
        }
    }
}
