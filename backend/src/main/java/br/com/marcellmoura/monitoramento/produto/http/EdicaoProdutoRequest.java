package br.com.marcellmoura.monitoramento.produto.http;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import br.com.marcellmoura.monitoramento.produto.domain.ProdutoEdicaoInvalidaException;
import br.com.marcellmoura.monitoramento.produto.application.AlteracaoProduto;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;

final class EdicaoProdutoRequest {
    private static final Set<String> CADASTRO = Set.of("nome", "categoria");
    private static final Set<String> ESTOQUE = Set.of("estoqueAtual", "estoqueMinimo");
    private static final Set<String> EDITAVEIS = Set.of("nome", "categoria", "unidade", "precoVenda", "ativo",
            "estoqueAtual", "estoqueMinimo");
    private static final Pattern DECIMAL = Pattern.compile("[0-9]{1,17}(\\.[0-9]{1,3})?");

    private EdicaoProdutoRequest() {}

    static AlteracaoProduto interpretar(Map<String, JsonNode> dados, Perfil perfil) {
        validarCampos(dados, perfil);
        return new AlteracaoProduto(versao(dados.get("versao")), textoOpcional(dados, "nome"),
                dados.containsKey("categoria"), categoria(dados.get("categoria")), unidade(dados),
                decimal(dados, "precoVenda"), estado(dados), decimal(dados, "estoqueAtual"), decimal(dados, "estoqueMinimo"));
    }

    private static void validarCampos(Map<String, JsonNode> dados, Perfil perfil) {
        Set<String> permitidos = switch (perfil) {
            case ADMINISTRADOR -> EDITAVEIS;
            case COMPRAS -> CADASTRO;
            case ESTOQUE -> ESTOQUE;
            case CONSULTA -> Set.of();
        };
        if (dados.size() < 2) throw new ProdutoEdicaoInvalidaException("Informe os campos a editar e a versão do produto.");
        for (String campo : dados.keySet()) {
            if ("versao".equals(campo)) continue;
            if (!EDITAVEIS.contains(campo)) throw new ProdutoEdicaoInvalidaException("O formulário contém um campo que não pode ser editado.");
            if (!permitidos.contains(campo)) throw new AccessDeniedException("Seu perfil não permite editar os campos enviados.");
        }
    }

    private static long versao(JsonNode valor) {
        if (valor == null || !valor.isIntegralNumber() || !valor.canConvertToLong() || valor.asLong() < 0) {
            throw new ProdutoEdicaoInvalidaException("Versão inválida. Reabra a edição do produto.");
        }
        return valor.asLong();
    }

    private static String texto(JsonNode valor, String campo) {
        if (valor == null || !valor.isString()) throw new ProdutoEdicaoInvalidaException("Informe um texto válido para " + campo + ".");
        return valor.asString();
    }

    private static String textoOpcional(Map<String, JsonNode> dados, String campo) {
        return dados.containsKey(campo) ? texto(dados.get(campo), campo) : null;
    }

    private static String categoria(JsonNode valor) {
        return valor == null || valor.isNull() ? null : texto(valor, "categoria");
    }

    private static Unidade unidade(Map<String, JsonNode> dados) {
        String valor = textoOpcional(dados, "unidade");
        if (valor == null) return null;
        try {
            return Unidade.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            throw new ProdutoEdicaoInvalidaException("Selecione uma unidade válida.");
        }
    }

    private static BigDecimal decimal(Map<String, JsonNode> dados, String campo) {
        String valor = textoOpcional(dados, campo);
        if (valor == null) return null;
        if (!DECIMAL.matcher(valor).matches()) throw new ProdutoEdicaoInvalidaException("Informe valores decimais não negativos.");
        return new BigDecimal(valor);
    }

    private static Boolean estado(Map<String, JsonNode> dados) {
        if (!dados.containsKey("ativo")) return null;
        JsonNode valor = dados.get("ativo");
        if (valor == null || !valor.isBoolean()) throw new ProdutoEdicaoInvalidaException("Selecione um estado válido.");
        return valor.asBoolean();
    }
}
