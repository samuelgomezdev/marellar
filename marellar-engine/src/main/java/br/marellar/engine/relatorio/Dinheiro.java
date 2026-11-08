package br.marellar.engine.relatorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formata valores em reais no padrao brasileiro (1.234.567,89), sem passar por double. */
public final class Dinheiro {

    private static final DecimalFormatSymbols SIMBOLOS = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR"));

    private Dinheiro() {
    }

    public static String formatar(BigDecimal valor) {
        DecimalFormat f = new DecimalFormat("#,##0.00", SIMBOLOS);
        f.setRoundingMode(RoundingMode.HALF_EVEN);
        return f.format(valor);
    }
}
