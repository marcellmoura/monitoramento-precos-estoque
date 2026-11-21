package br.com.marcellmoura.monitoramento.importacao.extracao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao.Dados;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao.Item;

final class InterpretadorCotacao {
    private static final Pattern DATA = Pattern.compile("data de referência:\\s*(\\d{2}/\\d{2}/\\d{4})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern VALOR = Pattern.compile("[+-]?(?:[0-9]{1,3}(?:\\.[0-9]{3})+|[0-9]+)(?:,[0-9]{1,2})?");
    private static final String VALOR_FICHA = "Valor de compra:";
    private static final String APRESENTACAO_FICHA = "Apresentação:";

    PreviaImportacao interpretar(String texto) throws ExtracaoPdfException {
        List<String> linhas = texto.lines().map(String::strip).filter(s -> !s.isBlank()).toList();
        var data = DATA.matcher(texto.replace('\t', ' '));
        String dataReferencia = data.find() ? data.group(1) : null;
        for (int i = 0; i < linhas.size(); i++) {
            Layout layout = Layout.identificar(linhas.get(i));
            if (layout != null) return montar(layout.name(), dataReferencia, tabela(linhas.subList(i + 1, linhas.size()), layout));
        }
        if (linhas.stream().anyMatch(linha -> linha.startsWith(VALOR_FICHA))
                && linhas.stream().anyMatch(linha -> linha.startsWith(APRESENTACAO_FICHA))) {
            return montar("FICHAS", dataReferencia, fichas(linhas));
        }
        throw new ExtracaoPdfException("Layout não reconhecido. Confira se a cotação usa um dos formatos disponíveis.");
    }

    private PreviaImportacao montar(String layout, String data, List<Item> itens) throws ExtracaoPdfException {
        if (itens.stream().noneMatch(item -> item.dados() != null)) {
            throw new ExtracaoPdfException("Nenhum item pôde ser lido nesse PDF.");
        }
        return new PreviaImportacao(layout, data, itens);
    }

    private List<Item> tabela(List<String> linhas, Layout layout) {
        return linhas.stream().filter(linha -> !linha.startsWith("DADOS FICTÍCIOS PARA TESTE"))
                .filter(linha -> !linha.equals("(R$)") && Layout.identificar(linha) == null)
                .map(linha -> linhaTabela(linha, layout)).toList();
    }

    private Item linhaTabela(String linha, Layout layout) {
        String[] c = linha.split("\t", -1);
        if (c.length != layout.colunas.size()) return ilegivel(linha);
        return switch (layout) {
            case KG_FARDO -> item(new Dados(c[0], c[1], c[2], null, null), precos(layout, c, 3), linha);
            case ATACADO_VAREJO -> item(new Dados(null, c[0], null, null, null), precos(layout, c, 1), linha);
            case TABELA -> item(new Dados(c[0], c[1], c[2], c[3], c[4]), precos(layout, c, 5), linha);
        };
    }

    private Map<String, String> precos(Layout layout, String[] colunas, int inicio) {
        Map<String, String> valores = new LinkedHashMap<>();
        for (int i = inicio; i < colunas.length; i++) valores.put(layout.colunas.get(i), colunas[i].strip());
        return valores;
    }

    private List<Item> fichas(List<String> linhas) {
        List<Item> itens = new ArrayList<>();
        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i);
            if (!linha.startsWith(VALOR_FICHA)) continue;
            String descricao = i == 0 ? "" : linhas.get(i - 1);
            String complementos = i + 1 < linhas.size() ? linhas.get(i + 1) : "";
            itens.add(ficha(descricao, linha, complementos));
        }
        return itens;
    }

    private Item ficha(String descricao, String linha, String complementos) {
        String bruto = descricao + "\n" + linha;
        if (!complementos.startsWith(APRESENTACAO_FICHA) || descricao.isBlank()
                || descricao.startsWith(APRESENTACAO_FICHA) || descricao.startsWith(VALOR_FICHA)) return ilegivel(bruto);
        Map<String, String> campos = new LinkedHashMap<>();
        for (String campo : (linha + "|" + complementos).split("\\|")) {
            int separador = campo.indexOf(':');
            if (separador >= 0) campos.put(campo.substring(0, separador).strip(), campo.substring(separador + 1).strip());
        }
        return item(new Dados(campos.get("Referência"), descricao, campos.get("Unidade"), campos.get("Apresentação"),
                campos.get("Quantidade cotada")), Map.of("Valor de compra", campos.getOrDefault("Valor de compra", "")),
                bruto + "\n" + complementos);
    }

    private Item item(Dados dados, Map<String, String> precos, String bruto) {
        if (dados.descricao().isBlank()) return ilegivel(bruto);
        List<String> pendencias = new ArrayList<>();
        if (dados.unidade() == null || dados.unidade().isBlank()) pendencias.add("Unidade não informada no PDF.");
        if ("Não informada".equalsIgnoreCase(dados.apresentacao())) pendencias.add("Apresentação não informada no PDF.");
        precos.forEach((coluna, valor) -> {
            if (!precoPositivo(valor)) pendencias.add("Preço ausente ou inválido em " + coluna + ".");
        });
        return new Item(dados, precos, bruto.replace('\t', ' '), pendencias);
    }

    private boolean precoPositivo(String valor) {
        String numero = valor.replace("R$", "").strip();
        return VALOR.matcher(numero).matches()
                && new BigDecimal(numero.replace(".", "").replace(',', '.')).signum() > 0;
    }

    private Item ilegivel(String linha) {
        return new Item(null, Map.of(), linha.replace('\t', ' '), List.of("Linha incompleta ou não reconhecida. Confira o PDF."));
    }

    private enum Layout {
        KG_FARDO("Ref. Interna", "Produto", "Unidade", "Preço KG", "Preço Fardo"),
        ATACADO_VAREJO("Descrição", "ATACADO", "VAREJO"),
        TABELA("Código", "Produto", "UN", "Apresentação", "Qtd.", "Preço");

        private final List<String> colunas;
        Layout(String... colunas) { this.colunas = List.of(colunas); }

        static Layout identificar(String linha) {
            var campos = Arrays.stream(linha.split("\t")).map(s -> s.strip().toLowerCase(Locale.ROOT)).toList();
            return Arrays.stream(values()).filter(layout -> layout.colunas.stream().map(s -> s.toLowerCase(Locale.ROOT))
                    .toList().equals(campos)).findFirst().orElse(null);
        }
    }
}
