package br.marellar.engine;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class RelogioTest {

    private static final LocalDate SEGUNDA_1_MAR_2027 = LocalDate.of(2027, 3, 1);

    @Test
    void comecaNoPrimeiroDiaAsSeteHoras() {
        Relogio r = new Relogio(SEGUNDA_1_MAR_2027);

        assertThat(r.data()).isEqualTo(SEGUNDA_1_MAR_2027);
        assertThat(r.hora()).isEqualTo(7);
        assertThat(r.diaUtilDoMes()).isEqualTo(1);
        assertThat(r.mes()).isZero();
        assertThat(r.mesReferencia()).isEqualTo(YearMonth.of(2027, 3));
    }

    @Test
    void oitoTicksFormamUmDiaUtil() {
        Relogio r = new Relogio(SEGUNDA_1_MAR_2027);
        for (int i = 0; i < 7; i++) {
            r.avancar();
        }
        assertThat(r.hora()).isEqualTo(14);
        assertThat(r.data()).isEqualTo(SEGUNDA_1_MAR_2027);

        r.avancar();

        assertThat(r.hora()).isEqualTo(7);
        assertThat(r.data()).isEqualTo(LocalDate.of(2027, 3, 2));
        assertThat(r.fimDeDia()).isTrue();
    }

    @Test
    void pulaOFimDeSemana() {
        // sexta, 5 de marco de 2027, e o 5o dia util; o 6o e a segunda, dia 8
        Relogio r = new Relogio(SEGUNDA_1_MAR_2027, 5L * Relogio.TICKS_POR_DIA);

        assertThat(r.data()).isEqualTo(LocalDate.of(2027, 3, 8));
    }

    @Test
    void inicioNoSabadoVaiParaSegunda() {
        Relogio r = new Relogio(LocalDate.of(2027, 3, 6));

        assertThat(r.data()).isEqualTo(SEGUNDA_1_MAR_2027.plusDays(7));
    }

    @Test
    void mesTem21DiasUteis() {
        Relogio r = new Relogio(SEGUNDA_1_MAR_2027, Relogio.TICKS_POR_MES - 1);
        assertThat(r.mes()).isZero();
        assertThat(r.diaUtilDoMes()).isEqualTo(21);
        assertThat(r.fimDeMes()).isFalse();

        r.avancar();

        assertThat(r.fimDeMes()).isTrue();
        assertThat(r.mes()).isEqualTo(1);
        assertThat(r.diaUtilDoMes()).isEqualTo(1);
        assertThat(r.mesReferencia()).isEqualTo(YearMonth.of(2027, 4));
        // 21 dias uteis depois de 1/3/2027 (seg) = 30/3/2027 (ter)
        assertThat(r.data()).isEqualTo(LocalDate.of(2027, 3, 30));
    }

    @Test
    void somarDiasUteisBateComContagemDiaADia() {
        LocalDate esperado = SEGUNDA_1_MAR_2027;
        for (int n = 0; n <= 300; n++) {
            assertThat(Relogio.somarDiasUteis(SEGUNDA_1_MAR_2027, n)).as("n=%d", n).isEqualTo(esperado);
            do {
                esperado = esperado.plusDays(1);
            } while (!Relogio.ehDiaUtil(esperado));
        }
    }
}
