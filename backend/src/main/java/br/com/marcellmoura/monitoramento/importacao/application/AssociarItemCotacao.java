package br.com.marcellmoura.monitoramento.importacao.application;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.importacao.domain.ConferenciaCotacao.Revisao;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao.Item;
import br.com.marcellmoura.monitoramento.importacao.domain.RevisaoItem;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.Identidade;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.ProdutoCatalogo;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.Situacao;
import br.com.marcellmoura.monitoramento.importacao.persistence.ConferenciaRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.RevisaoItemRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.VinculoItemRepository;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;
import br.com.marcellmoura.monitoramento.produto.persistence.ProdutoRepository;

@Service
@Transactional(readOnly = true)
public class AssociarItemCotacao {
    private final RegistroImportacoes importacoes;
    private final ConferenciaRepository conferencias;
    private final RevisaoItemRepository revisoes;
    private final VinculoItemRepository vinculos;
    private final ProdutoRepository produtos;

    public AssociarItemCotacao(RegistroImportacoes importacoes, ConferenciaRepository conferencias,
            RevisaoItemRepository revisoes, VinculoItemRepository vinculos, ProdutoRepository produtos) {
        this.importacoes = importacoes;
        this.conferencias = conferencias;
        this.revisoes = revisoes;
        this.vinculos = vinculos;
        this.produtos = produtos;
    }

    public List<Situacao> listar(long tentativa) {
        var itens = importacoes.previa(tentativa).itens();
        var base = conferencias.atual(tentativa).orElse(null);
        var revisados = revisoes.listar(tentativa).stream().collect(Collectors.toMap(RevisaoItem::linha, Function.identity()));
        var associados = vinculos.listar(tentativa).stream().collect(Collectors.toMap(VinculoItem::linha, Function.identity()));
        return IntStream.rangeClosed(1, itens.size())
                .mapToObj(linha -> situacao(linha, base, revisados.get(linha), itens.get(linha - 1), associados.get(linha)))
                .toList();
    }

    public Page<ProdutoCatalogo> pesquisar(Unidade unidade, String nome, int pagina) {
        return produtos.findByAtivoTrueAndUnidadeAndNomeContainingIgnoreCase(unidade, nome.strip(),
                PageRequest.of(pagina, 20, Sort.by("id"))).map(ProdutoCatalogo::de);
    }

    public record Escolha(long produto, long conferencia, long revisaoItem, long vinculo) {}

    @Transactional
    public VinculoItem associar(long tentativa, int linha, long ator, Escolha escolha) {
        conferencias.bloquearTentativa(tentativa);
        var atual = listar(tentativa).stream().filter(item -> item.linha() == linha).findFirst().orElseThrow(() ->
                new ImportacaoException(404, "ITEM_NAO_ENCONTRADO", "Item da prévia não encontrado."));
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

    private Situacao situacao(int linha, Revisao base, RevisaoItem revisado, Item bruto, VinculoItem vinculo) {
        var identidade = base == null ? null : identidade(base, revisado, bruto);
        boolean revalidar = vinculo != null && (identidade == null || !vinculo.identidade().mesmaApresentacao(identidade));
        return new Situacao(linha, base == null ? null : base.id(), revisado == null ? 0 : revisado.id(),
                identidade, vinculo, revalidar);
    }

    private Identidade identidade(Revisao base, RevisaoItem revisado, Item bruto) {
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
}
