package br.marellar.engine.estado;

import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.Linha;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.Velocidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Retrato imutavel da partida num instante. E o que a CLI imprime, a API devolve
 * e o teste de determinismo compara (records tem equals por valor).
 */
public record EstadoPartida(
        long semente,
        long tick,
        LocalDate data,
        int hora,
        int mes,
        String mesReferencia,
        int diaUtilDoMes,
        Velocidade velocidade,
        boolean pausado,
        MotivoPausa motivoPausa,
        List<BarcoVisao> barcos,
        List<PessoaVisao> pessoas,
        String barcoEmInspecao,
        List<String> filaInspecao,
        BigDecimal horasOciosas,
        long ultimoEventoId) {

    public record BarcoVisao(
            String id,
            Linha linha,
            String cliente,
            CodigoGate gateAtual,
            String nomeGate,
            StatusBarco status,
            boolean retido,
            int prioridade,
            BigDecimal hhTotalDoGate,
            BigDecimal hhRestantesNoGate,
            BigDecimal progressoGate,
            BigDecimal hhProduzidas,
            BigDecimal hhApontadas,
            List<CodigoGate> gatesAprovados,
            List<String> pessoasAlocadas) {}

    public record PessoaVisao(String id, String nome, Funcao funcao, int nivel, int moral, String alocadoEm) {}
}
