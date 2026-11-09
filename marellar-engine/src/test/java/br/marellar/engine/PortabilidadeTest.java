package br.marellar.engine;

import br.marellar.engine.relatorio.Dinheiro;
import br.marellar.engine.save.Sha256;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HexFormat;
import java.util.Locale;
import java.util.SplittableRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As versoes portateis (para o navegador) dao exatamente o mesmo resultado que as classes do JDK
 * que elas substituem: saves antigos continuam valendo.
 */
class PortabilidadeTest {

    @Test
    void aleatorioIgualAoSplittableRandom() {
        for (long semente : new long[]{0, 1, 42, -7, Long.MAX_VALUE, 123456789L}) {
            SplittableRandom jdk = new SplittableRandom(semente);
            Aleatorio nosso = new Aleatorio(semente);
            for (int i = 0; i < 20_000; i++) {
                switch (i % 4) {
                    case 0 -> assertThat(nosso.proximoInt(7)).isEqualTo(jdk.nextInt(7));
                    case 1 -> assertThat(nosso.proximoInt(1, 16)).isEqualTo(jdk.nextInt(1, 16));
                    case 2 -> assertThat(nosso.proximoInt(8)).isEqualTo(jdk.nextInt(8));
                    default -> {
                        BigDecimal p = new BigDecimal("0.37");
                        boolean esperado = BigDecimal.valueOf(jdk.nextDouble()).compareTo(p) < 0;
                        assertThat(nosso.sortear(p)).isEqualTo(esperado);
                    }
                }
            }
        }
    }

    @Test
    void sha256IgualAoMessageDigest() throws Exception {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            String s = texto.toString();
            String esperado = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
            assertThat(Sha256.hex(s)).isEqualTo(esperado);
            texto.append((char) ('a' + i % 26)).append(i % 7 == 0 ? "ção" : "");
        }
    }

    @Test
    void dinheiroIgualAoDecimalFormat() {
        DecimalFormat f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        f.setRoundingMode(java.math.RoundingMode.HALF_EVEN);
        for (String v : new String[]{"0", "0.005", "0.015", "-0.004", "-0.006", "1", "999.999", "1000", "-1234567.891",
                "12345678901234.5", "100", "-100000"}) {
            assertThat(Dinheiro.formatar(new BigDecimal(v))).as(v).isEqualTo(f.format(new BigDecimal(v)));
        }
    }
}
