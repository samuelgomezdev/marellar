package br.marellar.engine.regras;

import br.marellar.engine.Partida;
import br.marellar.engine.dominio.Barco;
import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.TipoEvento;

import java.util.Map;
import java.util.Optional;

/**
 * Inspecao de 1 tick. O inspetor atende um barco por vez, em ordem de chegada,
 * e leva {@code ticksInspecao} ticks por gate.
 * <p>
 * No M2 todo gate inspecionado e aprovado. O sorteio de NC e a deteccao pelo
 * inspetor entram no M3.
 */
public final class InspecaoRegra {

    private InspecaoRegra() {
    }

    public static void executar(Partida partida) {
        if (partida.barcoEmInspecao().isEmpty()) {
            Optional<String> proximo = partida.proximoDaFila();
            if (proximo.isEmpty()) {
                return;
            }
            Barco barco = partida.barco(proximo.get()).orElseThrow();
            barco.mudarStatus(StatusBarco.EM_INSPECAO);
            partida.iniciarInspecao(barco.id(), partida.parametros().linha33().ticksInspecao());
        }

        if (partida.trabalharInspecao()) {
            Barco barco = partida.barco(partida.barcoEmInspecao().orElseThrow()).orElseThrow();
            partida.encerrarInspecao();
            aprovarGate(partida, barco);
        }
    }

    static void aprovarGate(Partida partida, Barco barco) {
        barco.registrarAprovacao();
        partida.registrarEvento(TipoEvento.GATE_APROVADO, barco.id(), "eventos.gateAprovado",
                Map.of("barco", barco.id(), "gate", barco.gateAtual().name()));
        if (barco.retido()) {
            barco.mudarStatus(StatusBarco.APROVADO);
        } else {
            seguirParaProximoGate(partida, barco);
        }
    }

    /** Leva o barco aprovado para o proximo gate, ou o entrega se o gate era a entrega tecnica. */
    public static void seguirParaProximoGate(Partida partida, Barco barco) {
        if (barco.gateAtual().ultimo()) {
            barco.mudarStatus(StatusBarco.ENTREGUE);
            partida.registrarEvento(TipoEvento.BARCO_ENTREGUE, barco.id(), "eventos.barcoEntregue",
                    Map.of("barco", barco.id()));
            return;
        }
        CodigoGate proximo = barco.gateAtual().proximo();
        barco.iniciarGate(proximo, partida.parametros().linha33().hhDoGate(proximo));
    }
}
