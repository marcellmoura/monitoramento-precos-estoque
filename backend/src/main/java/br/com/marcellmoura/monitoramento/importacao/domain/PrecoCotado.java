package br.com.marcellmoura.monitoramento.importacao.domain;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class PrecoCotado {
    private static final Pattern VALOR = Pattern.compile("[+-]?(?:[0-9]{1,3}(?:\\.[0-9]{3})+|[0-9]+)(?:,[0-9]{1,2})?");

    private PrecoCotado() {}

    public static boolean positivo(String valor) {
        return lerPositivo(valor) != null;
    }

    public static BigDecimal lerPositivo(String valor) {
        if (valor == null) return null;
        String numero = valor.replace("R$", "").strip();
        if (!VALOR.matcher(numero).matches()) return null;
        var decimal = new BigDecimal(numero.replace(".", "").replace(',', '.'));
        return decimal.signum() > 0 ? decimal : null;
    }
}
