package br.com.marcellmoura.monitoramento.importacao.domain;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class PrecoCotado {
    private static final Pattern VALOR = Pattern.compile("[+-]?(?:[0-9]{1,3}(?:\\.[0-9]{3})+|[0-9]+)(?:,[0-9]{1,2})?");

    private PrecoCotado() {}

    public static boolean positivo(String valor) {
        if (valor == null) return false;
        String numero = valor.replace("R$", "").strip();
        return VALOR.matcher(numero).matches()
                && new BigDecimal(numero.replace(".", "").replace(',', '.')).signum() > 0;
    }
}
