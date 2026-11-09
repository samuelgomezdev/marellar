package br.marellar.engine;

import java.math.BigDecimal;

/**
 * Gerador da partida. E o mesmo algoritmo do {@code java.util.SplittableRandom} (SplitMix64),
 * reescrito aqui para dar exatamente a mesma sequencia em qualquer plataforma: JVM, Android e
 * navegador (o TeaVM nao tem SplittableRandom). Saves antigos continuam reproduzindo igual.
 */
public final class Aleatorio {

    private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;
    /** 2^53: o sorteio de Bernoulli usa os 53 bits altos, como o nextDouble do JDK. */
    private static final BigDecimal DOIS_A_53 = BigDecimal.valueOf(1L << 53);

    private long semente;

    public Aleatorio(long semente) {
        this.semente = semente;
    }

    private long proximaSemente() {
        return semente += GOLDEN_GAMMA;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static int mix32(long z) {
        z = (z ^ (z >>> 33)) * 0x62a9d9ed799705f5L;
        return (int) (((z ^ (z >>> 28)) * 0xcb24d0a5c88c35b3L) >>> 32);
    }

    public int proximoInt() {
        return mix32(proximaSemente());
    }

    public long proximoLong() {
        return mix64(proximaSemente());
    }

    /** Inteiro em [0, limite), igual a {@code SplittableRandom.nextInt(limite)}. */
    public int proximoInt(int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("limite deve ser positivo: " + limite);
        }
        int r = proximoInt();
        int m = limite - 1;
        if ((limite & m) == 0) {
            return r & m;
        }
        for (int u = r >>> 1; u + m - (r = u % limite) < 0; u = proximoInt() >>> 1) {
            // rejeita o vies do modulo
        }
        return r;
    }

    /** Inteiro em [origem, limite), igual a {@code SplittableRandom.nextInt(origem, limite)}. */
    public int proximoInt(int origem, int limite) {
        if (origem >= limite) {
            throw new IllegalArgumentException("origem deve ser menor que o limite: " + origem + " >= " + limite);
        }
        int r = proximoInt();
        int n = limite - origem;
        int m = n - 1;
        if ((n & m) == 0) {
            return (r & m) + origem;
        }
        if (n > 0) {
            for (int u = r >>> 1; u + m - (r = u % n) < 0; u = proximoInt() >>> 1) {
                // rejeita o vies do modulo
            }
            return r + origem;
        }
        while (r < origem || r >= limite) {
            r = proximoInt();
        }
        return r;
    }

    /**
     * Sorteio de Bernoulli: verdadeiro com a probabilidade informada (0 nunca, 1 sempre).
     * Consome um long, como o {@code nextDouble()} do JDK, e compara sem passar por double:
     * {@code (long >>> 11) / 2^53 < probabilidade}.
     */
    public boolean sortear(BigDecimal probabilidade) {
        long k = proximoLong() >>> 11;
        return BigDecimal.valueOf(k).compareTo(probabilidade.multiply(DOIS_A_53)) < 0;
    }
}
