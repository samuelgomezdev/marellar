package br.marellar.engine;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Tempo discreto do motor. Um tick = 1 hora de turno; o dia util tem 8 ticks
 * (1 turno no MVP) e o mes de jogo tem 21 dias uteis. Sabados e domingos sao pulados.
 * <p>
 * Tudo e calculado a partir do numero de ticks, entao o relogio nao acumula erro.
 */
public final class Relogio {

    public static final int TICKS_POR_DIA = 8;
    public static final int DIAS_UTEIS_POR_MES = 21;
    public static final int TICKS_POR_MES = TICKS_POR_DIA * DIAS_UTEIS_POR_MES;
    public static final int HORA_INICIO_TURNO = 7;

    private final LocalDate inicio;
    private long tick;

    public Relogio(LocalDate inicio) {
        this(inicio, 0);
    }

    public Relogio(LocalDate inicio, long tick) {
        this.inicio = proximoDiaUtil(inicio);
        this.tick = tick;
    }

    void avancar() {
        tick++;
    }

    public long tick() {
        return tick;
    }

    public LocalDate inicio() {
        return inicio;
    }

    /** Dias uteis completos desde o inicio da partida (0 no primeiro dia). */
    public long diaUtilAbsoluto() {
        return tick / TICKS_POR_DIA;
    }

    /** Hora dentro do turno, de 0 a 7. */
    public int horaDoTurno() {
        return (int) (tick % TICKS_POR_DIA);
    }

    /** Hora do relogio na parede, de 7 a 14. */
    public int hora() {
        return HORA_INICIO_TURNO + horaDoTurno();
    }

    /** Indice do mes de jogo, comecando em 0. */
    public int mes() {
        return (int) (tick / TICKS_POR_MES);
    }

    /** Dia util dentro do mes de jogo, de 1 a 21. */
    public int diaUtilDoMes() {
        return (int) (diaUtilAbsoluto() % DIAS_UTEIS_POR_MES) + 1;
    }

    /** Mes de referencia do mes de jogo (o 1o mes de jogo e o mes do inicio). */
    public YearMonth mesReferencia() {
        return YearMonth.from(inicio).plusMonths(mes());
    }

    private static final String[] MESES = {"jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"};

    /** Rotulo do mes de referencia, ex.: "mar/2027" (sem Locale: igual em qualquer plataforma). */
    public String rotuloMesReferencia() {
        YearMonth m = mesReferencia();
        return MESES[m.getMonthValue() - 1] + "/" + m.getYear();
    }

    public LocalDate data() {
        return somarDiasUteis(inicio, diaUtilAbsoluto());
    }

    public LocalDateTime agora() {
        return data().atTime(hora(), 0);
    }

    /** Verdadeiro quando o tick atual fecha um mes de jogo (antes do primeiro tick do mes seguinte). */
    public boolean fimDeMes() {
        return tick > 0 && tick % TICKS_POR_MES == 0;
    }

    public boolean fimDeDia() {
        return tick > 0 && tick % TICKS_POR_DIA == 0;
    }

    public static LocalDate somarDiasUteis(LocalDate diaUtil, long dias) {
        LocalDate data = diaUtil.plusWeeks(dias / 5);
        long resto = dias % 5;
        while (resto > 0) {
            data = data.plusDays(1);
            if (ehDiaUtil(data)) {
                resto--;
            }
        }
        return data;
    }

    static LocalDate proximoDiaUtil(LocalDate data) {
        LocalDate d = data;
        while (!ehDiaUtil(d)) {
            d = d.plusDays(1);
        }
        return d;
    }

    static boolean ehDiaUtil(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY;
    }
}
