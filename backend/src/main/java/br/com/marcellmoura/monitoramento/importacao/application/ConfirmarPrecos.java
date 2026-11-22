package br.com.marcellmoura.monitoramento.importacao.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.Conflito;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.Linha;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.Operacao;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.PrecoAnterior;
import br.com.marcellmoura.monitoramento.importacao.domain.ConfirmacaoImportacao.Resolucao;
import br.com.marcellmoura.monitoramento.importacao.domain.ConferenciaCotacao.Revisao;
import br.com.marcellmoura.monitoramento.importacao.domain.PrecoCotado;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao.Item;
import br.com.marcellmoura.monitoramento.importacao.domain.RevisaoItem;
import br.com.marcellmoura.monitoramento.importacao.domain.TentativaImportacao.Status;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.Identidade;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.ProdutoCatalogo;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.Situacao;
import br.com.marcellmoura.monitoramento.importacao.persistence.ConferenciaRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.PrecoConfirmadoRepository;
import br.com.marcellmoura.monitoramento.importacao.persistence.PrecoConfirmadoRepository.Chave;
import br.com.marcellmoura.monitoramento.importacao.persistence.PrecoConfirmadoRepository.NovoPreco;
import br.com.marcellmoura.monitoramento.importacao.persistence.RevisaoItemRepository;
import br.com.marcellmoura.monitoramento.produto.domain.Produto;
import br.com.marcellmoura.monitoramento.produto.persistence.ProdutoRepository;

@Service
@Transactional(readOnly = true)
public class ConfirmarPrecos {
    private static final String ORIGEM_MANUAL = "VERIFICACAO_MANUAL";
    private static final String CONFLITO = "Já há outro preço para o item, apresentação e modalidade nesta data comercial. Aguardando resolução.";
    private static final String CONFLITO_NO_PDF = "Há mais de uma linha pronta para a mesma apresentação e modalidade nesta cotação.";
    private final RegistroImportacoes importacoes;
    private final ConferenciaRepository conferencias;
    private final RevisaoItemRepository revisoes;
    private final AssociarItemCotacao associacoes;
    private final PrecoConfirmadoRepository precos;
    private final ProdutoRepository produtos;

    public ConfirmarPrecos(RegistroImportacoes importacoes, ConferenciaRepository conferencias, RevisaoItemRepository revisoes,
            AssociarItemCotacao associacoes, PrecoConfirmadoRepository precos, ProdutoRepository produtos) {
        this.importacoes = importacoes;
        this.conferencias = conferencias;
        this.revisoes = revisoes;
        this.associacoes = associacoes;
        this.precos = precos;
        this.produtos = produtos;
    }

    public ConfirmacaoImportacao consultar(long tentativa) {
        return new ConfirmacaoImportacao(avaliar(tentativa, produtos::findAllById).stream().map(Avaliacao::linha).toList());
    }

    @Transactional
    public ConfirmacaoImportacao confirmar(long tentativa, long ator) {
        conferencias.bloquearTentativa(tentativa);
        precos.bloquearFornecedor(tentativa);
        var avaliadas = avaliar(tentativa, this::bloquearProdutos);
        var resultado = avaliadas.stream().map(avaliacao -> gravar(tentativa, ator, avaliacao)).toList();
        if (avaliadas.stream().anyMatch(avaliacao -> avaliacao.linha().estado() == ConfirmacaoImportacao.Estado.PRONTO)) {
            atualizarEstado(tentativa, resultado);
        }
        return new ConfirmacaoImportacao(resultado);
    }

    @Transactional
    public void resolver(long tentativa, int linha, long ator, Resolucao resolucao) {
        validarMotivo(resolucao);
        conferencias.bloquearTentativa(tentativa);
        precos.bloquearFornecedor(tentativa);

        var avaliadas = avaliar(tentativa, this::bloquearProdutos);
        var avaliada = avaliadas.stream().filter(item -> item.linha().linha() == linha).findFirst()
                .orElseThrow(() -> new ImportacaoException(404, "ITEM_NAO_ENCONTRADO", "Item da cotação não encontrado."));
        var confirmado = avaliada.linha().confirmado();
        if (confirmado != null && precos.resolucaoRegistrada(confirmado.id(), resolucao)) return;

        var conflito = avaliada.linha().conflito();
        if (conflito == null || !conflito.revisao().equals(resolucao.revisao())) throw conflitoAlterado();
        var anterior = conflito.anteriores().stream().filter(preco -> preco.id() == resolucao.precoReferencia()).findFirst()
                .orElseThrow(ConfirmarPrecos::conflitoAlterado);

        var novo = avaliada.registro();
        int ordem = anterior.ordem() + (resolucao.operacao() == Operacao.DEPOIS ? 1 : 0);
        if (resolucao.operacao() == Operacao.SUBSTITUIR) precos.liberarOrdem(anterior.id());
        else precos.abrirPosicao(tentativa, novo, ordem);
        var salvo = precos.inserir(tentativa, ator, novo, ordem, resolucao);
        atualizarEstado(tentativa, avaliadas.stream()
                .map(item -> item.linha().linha() == linha ? Linha.confirmado(salvo) : item.linha()).toList());
    }

    private static void validarMotivo(Resolucao resolucao) {
        if (resolucao.operacao() == Operacao.SUBSTITUIR && resolucao.motivo() == null) {
            throw new ImportacaoException(400, "MOTIVO_OBRIGATORIO", "Informe o motivo da substituição.");
        }
        if (resolucao.operacao() != Operacao.SUBSTITUIR && resolucao.motivo() != null) {
            throw new ImportacaoException(400, "MOTIVO_INDEVIDO", "O motivo se aplica à substituição do preço.");
        }
    }

    private static ImportacaoException conflitoAlterado() {
        return new ImportacaoException(409, "CONFLITO_PRECO_ALTERADO",
                "O conflito mudou. Confira novamente os preços e confirme uma nova escolha.");
    }

    private void atualizarEstado(long tentativa, List<Linha> resultado) {
        var concluida = resultado.stream().allMatch(linha -> linha.confirmado() != null);
        precos.atualizarEstado(tentativa, concluida ? Status.CONCLUIDA : Status.CONCLUIDA_COM_PENDENCIAS);
    }

    private List<Produto> bloquearProdutos(List<Long> ids) {
        return ids.stream().sorted().flatMap(id -> produtos.buscarParaVinculo(id).stream()).toList();
    }

    private Linha gravar(long tentativa, long ator, Avaliacao avaliacao) {
        if (avaliacao.linha().estado() != ConfirmacaoImportacao.Estado.PRONTO) return avaliacao.linha();
        return Linha.confirmado(precos.inserir(tentativa, ator, avaliacao.registro(), 1, null));
    }

    private record Avaliacao(Linha linha, NovoPreco registro) {
        static Avaliacao pendente(int linha, List<String> motivos) {
            return new Avaliacao(Linha.pendente(linha, motivos), null);
        }
    }

    private List<Avaliacao> avaliar(long tentativa, Function<List<Long>, List<Produto>> consultarProdutos) {
        var previa = importacoes.previa(tentativa);
        var base = conferencias.atual(tentativa).orElse(null);
        var confirmados = precos.listar(tentativa);
        var corrigidos = AssociarItemCotacao.porLinha(revisoes.listar(tentativa));
        var situacoes = associacoes.listar(tentativa);
        var ids = situacoes.stream().filter(item -> !confirmados.containsKey(item.linha()))
                .map(ConfirmarPrecos::produto).filter(Objects::nonNull).map(ProdutoCatalogo::id).distinct().toList();
        var catalogo = consultarProdutos.apply(ids).stream().collect(Collectors.toMap(Produto::getId, Function.identity()));
        var avaliadas = situacoes.stream().map(situacao -> {
            int linha = situacao.linha();
            var confirmado = confirmados.get(linha);
            return confirmado != null ? new Avaliacao(Linha.confirmado(confirmado), null)
                    : avaliarLinha(situacao, previa.itens().get(linha - 1), corrigidos.get(linha), base, catalogo);
        }).toList();
        return base == null ? avaliadas : verificarConflitos(tentativa, base, avaliadas);
    }

    private List<Avaliacao> verificarConflitos(long tentativa, Revisao base, List<Avaliacao> avaliadas) {
        var vigentes = precos.vigentes(tentativa, base.dataComercial());
        var repeticoes = avaliadas.stream().map(Avaliacao::registro).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(NovoPreco::chave, Collectors.counting()));
        return avaliadas.stream().map(avaliacao -> {
            var registro = avaliacao.registro();
            if (registro == null) return avaliacao;
            if (repeticoes.get(registro.chave()) > 1) return Avaliacao.pendente(registro.linha(), List.of(CONFLITO_NO_PDF));
            var anteriores = vigentes.get(registro.chave());
            if (anteriores == null) return avaliacao;
            var conflito = new Conflito(registro.preco(), registro.data(), anteriores, revisaoConflito(registro, anteriores));
            return new Avaliacao(Linha.emConflito(registro.linha(), CONFLITO, conflito), registro);
        }).toList();
    }

    private static String revisaoConflito(NovoPreco registro, List<PrecoAnterior> anteriores) {
        var ids = anteriores.stream().map(PrecoAnterior::id).toList();
        var revisao = "%d/%d/%s/%d/%d/%s".formatted(registro.linha(), registro.conferencia(), registro.revisao(),
                registro.vinculo(), registro.produto(), ids);
        return UUID.nameUUIDFromBytes(revisao.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Avaliacao avaliarLinha(Situacao situacao, Item bruto, RevisaoItem corrigido, Revisao base, Map<Long, Produto> catalogo) {
        int linha = situacao.linha();
        if (base == null) return Avaliacao.pendente(linha, List.of("Salve a conferência comercial."));
        var identidade = situacao.identidade();
        var escolhido = produto(situacao);
        var motivos = new ArrayList<String>();
        if (identidade == null) motivos.add("Confira a unidade e a embalagem do item.");
        if (escolhido == null) motivos.add("Confirme a associação ao produto do catálogo.");
        else if (!produtoValido(catalogo.get(escolhido.id()), identidade)) motivos.add("Selecione um produto ativo com a mesma unidade.");

        BigDecimal preco = corrigido == null ? PrecoCotado.lerPositivo(bruto.precos().get(base.base().coluna())) : corrigido.dados().preco();
        if (!precoValido(preco)) motivos.add("Informe um preço de compra positivo e válido.");
        if (!motivos.isEmpty()) return Avaliacao.pendente(linha, motivos);

        var modalidade = corrigido == null ? base.base().coluna() : corrigido.dados().coluna();
        var chave = Chave.de(identidade, escolhido.id(), modalidade == null ? ORIGEM_MANUAL : modalidade);
        long origem = situacao.reaproveitado() == null ? situacao.vinculo().id() : situacao.reaproveitado().vinculoOrigem();
        var novo = new NovoPreco(linha, base.id(), corrigido == null ? null : corrigido.id(), origem,
                escolhido.id(), identidade, chave, base.dataComercial(), preco);
        return new Avaliacao(Linha.pronto(linha, preco), novo);
    }

    private boolean precoValido(BigDecimal preco) {
        return preco != null && preco.signum() > 0 && preco.scale() <= 2 && preco.precision() - preco.scale() <= 12;
    }

    private boolean produtoValido(Produto produto, Identidade identidade) {
        return produto != null && produto.isAtivo() && identidade != null && produto.getUnidade() == identidade.unidade();
    }

    private static ProdutoCatalogo produto(Situacao situacao) {
        if (situacao.reaproveitado() != null) return situacao.reaproveitado().produto();
        return situacao.vinculo() == null || situacao.revalidar() ? null : situacao.vinculo().produto();
    }
}
