package br.marellar.engine.agentes;

import br.marellar.engine.dados.CatalogoNc;
import br.marellar.engine.dados.ParametrosPessoas.Personalidade;
import br.marellar.engine.dominio.Gravidade;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * O inspetor, unico agente com personalidade no MVP (README 5.5).
 * <ul>
 *   <li>Mais rigor: detecta mais e demora mais.</li>
 *   <li>Mais velocidade: demora menos e detecta um pouco menos.</li>
 *   <li>Pouca paciencia: cada concessao derruba mais a moral dele.</li>
 * </ul>
 */
public final class Inspetor {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final BigDecimal DOIS = BigDecimal.valueOf(2);
    private static final int ESCALA = 4;

    private Inspetor() {
    }

    /** deteccaoBase[gravidade] x fatorPersonalidade x fatorCarga, limitado a [0, 1]. */
    public static BigDecimal probabilidadeDeteccao(Gravidade gravidade, Personalidade p, int barcosNaFila,
                                                   CatalogoNc catalogo) {
        BigDecimal prob = catalogo.deteccaoBase().get(gravidade)
                .multiply(fatorPersonalidade(p, catalogo.inspecao()))
                .multiply(fatorCarga(barcosNaFila, catalogo.inspecao()));
        return limitar(prob);
    }

    public static BigDecimal fatorPersonalidade(Personalidade p, CatalogoNc.Inspecao i) {
        return i.deteccaoBase()
                .add(i.pesoRigorNaDeteccao().multiply(pct(p.rigor())))
                .subtract(i.pesoVelocidadeNaDeteccao().multiply(pct(p.velocidade())))
                .setScale(ESCALA, RoundingMode.HALF_EVEN);
    }

    /** 1 ate {@code filaSemPerda} barcos esperando; depois cai {@code perdaPorBarcoNaFila} por barco, ate o minimo. */
    public static BigDecimal fatorCarga(int barcosNaFila, CatalogoNc.Inspecao i) {
        int excesso = barcosNaFila - i.filaSemPerda();
        if (excesso <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.ONE.subtract(i.perdaPorBarcoNaFila().multiply(BigDecimal.valueOf(excesso)))
                .max(i.fatorCargaMinimo());
    }

    /** Ticks para inspecionar um gate, a partir do tempo padrao da linha. Minimo de 1. */
    public static int ticksInspecao(int ticksPadrao, Personalidade p, CatalogoNc.Inspecao i) {
        BigDecimal fatorRigor = BigDecimal.ONE
                .subtract(i.pesoRigorNoTempo().divide(DOIS, ESCALA, RoundingMode.HALF_EVEN))
                .add(i.pesoRigorNoTempo().multiply(pct(p.rigor())));
        BigDecimal fatorVelocidade = BigDecimal.ONE
                .add(i.pesoVelocidadeNoTempo().divide(DOIS, ESCALA, RoundingMode.HALF_EVEN))
                .subtract(i.pesoVelocidadeNoTempo().multiply(pct(p.velocidade())));
        int ticks = BigDecimal.valueOf(ticksPadrao).multiply(fatorRigor).multiply(fatorVelocidade)
                .setScale(0, RoundingMode.HALF_UP).intValue();
        return Math.max(1, ticks);
    }

    /** Quanto a moral do inspetor cai a cada concessao: (100 - paciencia) x quedaMoralPorConcessao. */
    public static int quedaMoralPorConcessao(Personalidade p, CatalogoNc.Inspecao i) {
        return BigDecimal.valueOf(100L - p.paciencia()).multiply(i.quedaMoralPorConcessao())
                .setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static BigDecimal pct(int valor) {
        return BigDecimal.valueOf(valor).divide(CEM, ESCALA, RoundingMode.HALF_EVEN);
    }

    static BigDecimal limitar(BigDecimal prob) {
        return prob.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(ESCALA, RoundingMode.HALF_EVEN);
    }
}
