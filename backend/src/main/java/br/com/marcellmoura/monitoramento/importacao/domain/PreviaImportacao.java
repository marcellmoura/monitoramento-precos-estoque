package br.com.marcellmoura.monitoramento.importacao.domain;

import java.util.List;
import java.util.Map;

public record PreviaImportacao(String layout, String dataReferencia, List<Item> itens) {
    public record Dados(String referencia, String descricao, String unidade, String apresentacao, String quantidade) {}

    public record Item(Dados dados, Map<String, String> precos, String textoOriginal, List<String> pendencias) {}
}
