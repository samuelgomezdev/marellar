package br.marellar.engine.agentes;

import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.CatalogoNc;
import br.marellar.engine.dados.ParametrosPessoas.Personalidade;
import br.marellar.engine.dominio.Gravidade;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class InspetorTest {

    private static final CatalogoNc CATALOGO = CarregadorDados.carregarDoClasspath().catalogoNc();
    private static final CatalogoNc.Inspecao REGRAS = CATALOGO.inspecao();

    @Test
    void maisRigorDetectaMais() {
        BigDecimal rigoroso = Inspetor.probabilidadeDeteccao(Gravidade.MEDIA, new Personalidade(90, 50, 50), 0, CATALOGO);
        BigDecimal relaxado = Inspetor.probabilidadeDeteccao(Gravidade.MEDIA, new Personalidade(20, 50, 50), 0, CATALOGO);

        assertThat(rigoroso).isGreaterThan(relaxado);
    }

    @Test
    void maisVelocidadeDetectaUmPoucoMenos() {
        BigDecimal rapido = Inspetor.probabilidadeDeteccao(Gravidade.MEDIA, new Personalidade(50, 100, 50), 0, CATALOGO);
        BigDecimal lento = Inspetor.probabilidadeDeteccao(Gravidade.MEDIA, new Personalidade(50, 0, 50), 0, CATALOGO);

        assertThat(rapido).isLessThan(lento);
    }

    @Test
    void deteccaoDaMartaNumaNcMedia() {
        // 0,80 x (0,85 + 0,30 x 0,75 - 0,10 x 0,50) = 0,80 x 1,025 = 0,82
        BigDecimal p = Inspetor.probabilidadeDeteccao(Gravidade.MEDIA, new Personalidade(75, 50, 40), 0, CATALOGO);

        assertThat(p).isEqualByComparingTo("0.82");
    }

    @Test
    void probabilidadeNuncaPassaDeUm() {
        BigDecimal p = Inspetor.probabilidadeDeteccao(Gravidade.CRITICA, new Personalidade(100, 0, 50), 0, CATALOGO);

        assertThat(p).isEqualByComparingTo("1");
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "2, 1", "3, 0.90", "4, 0.80", "10, 0.60"})
    void filaGrandeDerrubaADeteccao(int fila, String esperado) {
        assertThat(Inspetor.fatorCarga(fila, REGRAS)).isEqualByComparingTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            // rigor, velocidade, ticks (padrao 2)
            "75,  50, 2",
            "100,  0, 3",
            "0,  100, 1",
            "50,  50, 2"
    })
    void tempoDeInspecaoDependeDaPersonalidade(int rigor, int velocidade, int ticks) {
        assertThat(Inspetor.ticksInspecao(2, new Personalidade(rigor, velocidade, 50), REGRAS)).isEqualTo(ticks);
    }

    @ParameterizedTest
    @CsvSource({"40, 6", "100, 0", "0, 10"})
    void poucaPacienciaDerrubaMaisAMoral(int paciencia, int queda) {
        assertThat(Inspetor.quedaMoralPorConcessao(new Personalidade(50, 50, paciencia), REGRAS)).isEqualTo(queda);
    }
}
