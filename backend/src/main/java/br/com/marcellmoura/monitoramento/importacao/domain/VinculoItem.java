package br.com.marcellmoura.monitoramento.importacao.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import br.com.marcellmoura.monitoramento.produto.domain.Produto;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

public record VinculoItem(long id, int linha, ProdutoCatalogo produto, Identidade identidade) {
    public record ProdutoCatalogo(long id, String codigo, String nome, Unidade unidade) {
        public static ProdutoCatalogo de(Produto produto) {
            return new ProdutoCatalogo(produto.getId(), produto.getCodigo(), produto.getNome(), produto.getUnidade());
        }
    }

    public record Identidade(String referencia, String descricao, Unidade unidade, String embalagem) {
        private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
        private static final Pattern ESPACOS = Pattern.compile("\\s+");

        public boolean mesmaIdentificacao(Identidade outra) {
            return mesmaApresentacao(outra) && normalizar(descricao).equals(normalizar(outra.descricao));
        }

        private static String normalizar(String texto) {
            var semMarcas = MARCAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
            return ESPACOS.matcher(semMarcas.strip().toLowerCase(Locale.ROOT)).replaceAll(" ");
        }

        public boolean mesmaApresentacao(Identidade outra) {
            if (unidade != outra.unidade || !embalagem.equalsIgnoreCase(outra.embalagem)) return false;
            return referencia == null ? outra.referencia == null && descricao.equalsIgnoreCase(outra.descricao)
                    : referencia.equals(outra.referencia);
        }
    }

    public record Reaproveitamento(long tentativaOrigem, long vinculoOrigem, ProdutoCatalogo produto) {}

    public record Situacao(int linha, Long conferencia, long revisaoItem, Identidade identidade, VinculoItem vinculo,
            boolean revalidar, Reaproveitamento reaproveitado) {}
}
