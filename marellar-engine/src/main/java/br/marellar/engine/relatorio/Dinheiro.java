package br.marellar.engine.relatorio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formata valores em reais no padrao brasileiro (1.234.567,89), sem passar por double.
 * Feito a mao (sem DecimalFormat nem Locale) para dar o mesmo texto na JVM, no Android e no navegador.
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static String formatar(BigDecimal valor) {
        BigDecimal arredondado = valor.setScale(2, RoundingMode.HALF_EVEN);
        String plano = arredondado.abs().toPlainString();
        int ponto = plano.indexOf('.');
        String inteiro = plano.substring(0, ponto);
        StringBuilder saida = new StringBuilder();
        // O sinal vem do valor original, como no DecimalFormat (-0,004 vira "-0,00"): o texto
        // entra nas mensagens de evento e na assinatura do save.
        if (valor.signum() < 0) {
            saida.append('-');
        }
        for (int i = 0; i < inteiro.length(); i++) {
            if (i > 0 && (inteiro.length() - i) % 3 == 0) {
                saida.append('.');
            }
            saida.append(inteiro.charAt(i));
        }
        return saida.append(',').append(plano, ponto + 1, plano.length()).toString();
    }
}
