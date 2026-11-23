package br.com.marcellmoura.monitoramento.importacao.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.importacao.domain.ConferenciaCotacao.Revisao;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao.Item;
import br.com.marcellmoura.monitoramento.importacao.domain.RevisaoItem;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.Identidade;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.ProdutoCatalogo;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.Situacao;
import br.com.marcellmoura.monitoramento.importacao.persistence.ConferenciaRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.PrecoConfirmadoRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.RevisaoItemRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.VinculoItemRepository;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;
import br.com.marcellmoura.monitoramento.produto.persistence.ProdutoRepository;

@Service
@Transactional(readOnly = true)
public class AssociarItemCotacao {
    private final RegistroImportacoes importacoes;
    private final PrecoConfirmadoRepository precos;
    private final ConferenciaRepository conferencias;
    private final RevisaoItemRepository revisoes;
    private final VinculoItemRepository vinculos;
    private final ProdutoRepository produtos;
    private final ReaproveitarVinculos reaproveitamento;

    public AssociarItemCotacao(RegistroImportacoes importacoes, ConferenciaRepository conferencias,
            RevisaoItemRepository revisoes, VinculoItemRepository vinculos, ProdutoRepository produtos,
            ReaproveitarVinculos reaproveitamento, PrecoConfirmadoRepository precos) {
        this.importacoes = importacoes;
        this.precos = precos;
        this.conferencias = conferencias;
        this.revisoes = revisoes;
        this.vinculos = vinculos;
        this.produtos = produtos;
        this.reaproveitamento = reaproveitamento;
    }

    public List<Situacao> listar(long tentativa) {
        return listar(tentativa, true);
    }

    private List<Situacao> listar(long tentativa, boolean incluirReaproveitamento) {
        return listar(tentativa, importacoes.previa(tentativa), conferencias.atual(tentativa).orElse(null),
                porLinha(revisoes.listar(tentativa)), incluirReaproveitamento);
    }

    List<Situacao> listar(long tentativa, PreviaImportacao previa, Revisao base, Map<Integer, RevisaoItem> revisados,
            boolean incluirReaproveitamento) {
        var associados = vinculos.listar(tentativa).stream().collect(Collectors.toMap(VinculoItem::linha, Function.identity()));
        var linhas = linhas(previa, base, revisados);
        Map<Integer, VinculoItem.Reaproveitamento> reutilizados = incluirReaproveitamento
                ? reaproveitamento.resolver(tentativa, linhas, associados.keySet()) : Map.of();
        return linhas.stream().map(linha -> situacao(linha, base, associados.get(linha.numero()),
                reutilizados.get(linha.numero()))).toList();
    }

    static Map<Integer, RevisaoItem> porLinha(List<RevisaoItem> revisoes) {
        return revisoes.stream().collect(Collectors.toMap(RevisaoItem::linha, Function.identity()));
    }

    static List<Linha> linhas(PreviaImportacao previa, Revisao base, Map<Integer, RevisaoItem> revisados) {
        return IntStream.rangeClosed(1, previa.itens().size())
                .mapToObj(linha -> Linha.de(linha, base, revisados.get(linha), previa.itens().get(linha - 1))).toList();
    }

    public Page<ProdutoCatalogo> pesquisar(Unidade unidade, String nome, int pagina) {
        return produtos.findByAtivoTrueAndUnidadeAndNomeContainingIgnoreCase(unidade, nome.strip(),
                PageRequest.of(pagina, 20, Sort.by("id"))).map(ProdutoCatalogo::de);
    }

    public record Escolha(long produto, long conferencia, long revisaoItem, long vinculo) {}

    @Transactional
    public VinculoItem associar(long tentativa, int linha, long ator, Escolha escolha) {
        conferencias.bloquearTentativa(tentativa);
        var atual = listar(tentativa, false).stream().filter(item -> item.linha() == linha).findFirst().orElseThrow(() ->
                new ImportacaoException(404, "ITEM_NAO_ENCONTRADO", "Item da prévia não encontrado."));
        if (precos.confirmado(tentativa, linha)) {
            throw new ImportacaoException(409, "PRECO_JA_CONFIRMADO", "O preço desta linha já foi confirmado no histórico.");
        }
        var identidade = atual.identidade();
        if (identidade == null) {
            throw new ImportacaoException(409, "APRESENTACAO_PENDENTE", "Confira a unidade e a embalagem antes de associar.");
        }
        if (atual.conferencia() != escolha.conferencia() || atual.revisaoItem() != escolha.revisaoItem()) throw desatualizado();
        var anterior = atual.vinculo();
        if (anterior != null && !atual.revalidar() && anterior.produto().id() == escolha.produto()) return anterior;
        if (escolha.vinculo() != (anterior == null ? 0 : anterior.id())) throw desatualizado();
        var produto = produtos.buscarParaVinculo(escolha.produto()).orElseThrow(() ->
                new ImportacaoException(404, "PRODUTO_NAO_ENCONTRADO", "Produto do catálogo não encontrado."));
        if (!produto.isAtivo() || produto.getUnidade() != identidade.unidade()) {
            throw new ImportacaoException(409, "PRODUTO_INCOMPATIVEL", "Selecione um produto ativo com a mesma unidade do item.");
        }
        produto.bloquearUnidade();
        produtos.flush();
        Long revisao = atual.revisaoItem() == 0 ? null : atual.revisaoItem();
        long id = vinculos.inserir(tentativa, linha, atual.conferencia(), revisao, ator, produto.getId(), identidade);
        return new VinculoItem(id, linha, ProdutoCatalogo.de(produto), identidade);
    }

    private ImportacaoException desatualizado() {
        return new ImportacaoException(409, "VINCULO_DESATUALIZADO", "O item ou o vínculo mudou. Atualize a página antes de associar.");
    }

    private Situacao situacao(Linha linha, Revisao base, VinculoItem vinculo, VinculoItem.Reaproveitamento reaproveitado) {
        var identidade = linha.identidade();
        boolean revalidar = vinculo != null && (identidade == null || !vinculo.identidade().mesmaApresentacao(identidade));
        return new Situacao(linha.numero(), base == null ? null : base.id(), linha.revisao(), identidade, vinculo,
                revalidar, reaproveitado);
    }

    record Linha(int numero, long revisao, Identidade identidade, String modalidade) {
        static Linha de(int numero, Revisao base, RevisaoItem revisado, Item bruto) {
            var modalidade = revisado != null ? revisado.dados().coluna() : base == null ? null : base.base().coluna();
            return new Linha(numero, revisado == null ? 0 : revisado.id(), identidade(base, revisado, bruto), modalidade);
        }

        private static Identidade identidade(Revisao base, RevisaoItem revisado, Item bruto) {
            if (base == null) return null;
            if (revisado != null) {
                var dados = revisado.dados();
                if (dados.unidade() == null || dados.embalagem() == null) return null;
                return new Identidade(dados.referencia(), dados.descricao(), dados.unidade(), dados.embalagem());
            }
            var dados = bruto.dados();
            var escolha = base.base();
            if (dados == null || !escolha.unidade().name().equalsIgnoreCase(dados.unidade())
                    || !escolha.embalagem().equalsIgnoreCase(dados.apresentacao())) return null;
            String referencia = dados.referencia() == null || dados.referencia().isBlank() ? null : dados.referencia().strip();
            return new Identidade(referencia, dados.descricao(), escolha.unidade(), escolha.embalagem());
        }

        boolean reutilizavel() {
            return modalidade != null && identidade != null && identidade.referencia() != null;
        }
    }
}
