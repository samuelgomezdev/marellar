package br.marellar.engine.regras;

import br.marellar.engine.Partida;
import br.marellar.engine.agentes.Inspetor;
import br.marellar.engine.dominio.NaoConformidade;
import br.marellar.engine.dominio.Barco;
import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.TipoEvento;

import java.util.Map;
import java.util.Optional;

/**
 * Inspecao de 1 tick. O inspetor atende um barco por vez, em ordem de chegada,
 * e o tempo depende da personalidade dele (rigor e velocidade). Ao terminar,
 * a QualidadeRegra decide o que foi detectado.
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
            int ticks = Inspetor.ticksInspecao(partida.parametros().linha33().ticksInspecao(),
                    partida.inspetor().personalidade().orElseThrow(),
                    partida.parametros().catalogoNc().inspecao());
            partida.iniciarInspecao(barco.id(), ticks);
        }

        if (partida.trabalharInspecao()) {
            Barco barco = partida.barco(partida.barcoEmInspecao().orElseThrow()).orElseThrow();
            partida.encerrarInspecao();
            QualidadeRegra.aposInspecao(partida, barco);
        }
    }

    /** Aprova o gate atual. Nunca aprova com NC pendente (esperando inspecao, decisao ou retrabalho). */
    static void aprovarGate(Partida partida, Barco barco) {
        if (partida.ncsDoGateAtual(barco).stream().anyMatch(NaoConformidade::pendente)) {
            throw new IllegalStateException("Gate " + barco.gateAtual() + " de " + barco.id() + " tem NC pendente");
        }
        barco.registrarAprovacao();
        FinanceiroRegra.gateAprovado(partida, barco);
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
        if (FornecedorRegra.consumirMaterial(partida, barco)) {
            EventoRegra.talvezQuebrePeca(partida, barco);
        }
    }
}
