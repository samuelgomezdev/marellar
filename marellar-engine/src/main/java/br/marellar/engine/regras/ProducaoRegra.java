package br.marellar.engine.regras;

import br.marellar.engine.Partida;
import br.marellar.engine.dados.ParametrosPessoas;
import br.marellar.engine.dominio.Barco;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.Pessoa;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.TipoEvento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Map;

/**
 * Producao de 1 tick (1 hora de turno).
 * <ul>
 *   <li>As pessoas de producao livres vao para o barco de maior prioridade, ate {@code maxPessoasPorBarco}.</li>
 *   <li>Cada pessoa produz {@code 1 h x fatorNivel x fatorMoral} HH padrao.</li>
 *   <li>Quando as HH do gate acabam, sorteiam-se as NCs e o barco vai para a fila de inspecao.</li>
 *   <li>Barco em retrabalho tambem recebe gente; ao terminar, o gate e aprovado.</li>
 * </ul>
 */
public final class ProducaoRegra {

    static final int ESCALA_HH = 4;
    private static final BigDecimal MORAL_BASE = new BigDecimal("0.7");
    private static final BigDecimal MORAL_PESO = new BigDecimal("0.003"); // 0,3 / 100

    private ProducaoRegra() {
    }

    public static void executar(Partida partida) {
        ParametrosPessoas regrasPessoas = partida.parametros().pessoas();
        int maxPorBarco = partida.parametros().linha33().maxPessoasPorBarco();

        // barcos parados tentam pegar o kit que chegou; quem continua parado registra hora improdutiva
        for (Barco barco : partida.barcosPorPrioridade()) {
            if (barco.status() == StatusBarco.PARADO_MATERIAL && !FornecedorRegra.consumirMaterial(partida, barco)) {
                barco.somarHoraParadoMaterial();
            }
        }

        Deque<Pessoa> livres = new ArrayDeque<>();
        partida.pessoas().stream()
                .filter(p -> p.funcao() == Funcao.PRODUCAO)
                .peek(Pessoa::liberar)
                .filter(Pessoa::disponivel)
                .sorted(Comparator.comparing(Pessoa::id))
                .forEach(livres::add);

        for (Barco barco : partida.barcosPorPrioridade()) {
            if (!barco.trabalhando() || livres.isEmpty()) {
                continue;
            }
            BigDecimal producao = BigDecimal.ZERO;
            int horasPagas = 0;
            int somaNiveis = 0;
            java.util.List<Pessoa> naBaia = new java.util.ArrayList<>();
            while (horasPagas < maxPorBarco && !livres.isEmpty()) {
                Pessoa p = livres.pollFirst();
                p.alocar(barco.id());
                naBaia.add(p);
                producao = producao.add(produtividade(p, regrasPessoas));
                somaNiveis += p.nivel();
                horasPagas++;
            }
            boolean retrabalho = barco.status() == StatusBarco.EM_RETRABALHO;
            barco.produzir(producao, horasPagas, somaNiveis);
            partida.somarHorasTrabalhadas(horasPagas);
            boolean lotada = naBaia.size() >= maxPorBarco;
            for (Pessoa p : naBaia) {
                EventoRegra.talvezAcidente(partida, p, barco, lotada);
            }
            if (!barco.trabalhoTerminado()) {
                continue;
            }
            if (retrabalho) {
                QualidadeRegra.retrabalhoConcluido(partida, barco);
            } else {
                QualidadeRegra.sortearNcs(partida, barco);
                barco.mudarStatus(StatusBarco.AGUARDANDO_INSPECAO);
                partida.entrarNaFilaDeInspecao(barco.id());
                partida.registrarEvento(TipoEvento.GATE_CONCLUIDO, barco.id(), "eventos.gateConcluido",
                        Map.of("barco", barco.id(), "gate", barco.gateAtual().name()));
            }
        }
        partida.somarHorasOciosas(livres.size());
    }

    /** HH padrao produzidas por 1 hora da pessoa: fatorNivel x (0,7 + 0,3 x moral/100). */
    public static BigDecimal produtividade(Pessoa p, ParametrosPessoas regras) {
        return regras.fatorDoNivel(p.nivel())
                .multiply(fatorMoral(p.moral()))
                .setScale(ESCALA_HH, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal fatorMoral(int moral) {
        return MORAL_BASE.add(MORAL_PESO.multiply(BigDecimal.valueOf(moral)));
    }
}
