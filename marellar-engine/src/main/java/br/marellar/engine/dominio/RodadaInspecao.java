package br.marellar.engine.dominio;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Rodada curta de inspecao de um barco num gate. Preparada quando o gate termina; o jogador responde com os
 * cliques (ordem InspecionarRodada) ou deixa com o inspetor. Depois da resposta, o barco espera
 * {@code ticksRestantes} (falsos alarmes) e o gate segue como numa inspecao normal.
 */
public final class RodadaInspecao {

    private final String barcoId;
    private final CodigoGate gate;
    private final List<Marca> marcas;
    private Map<String, ResultadoMarca> resultado;
    private int ticksRestantes;
    private boolean concluida;

    public RodadaInspecao(String barcoId, CodigoGate gate, List<Marca> marcas) {
        this.barcoId = Objects.requireNonNull(barcoId);
        this.gate = Objects.requireNonNull(gate);
        this.marcas = List.copyOf(marcas);
    }

    public void responder(Map<String, ResultadoMarca> resultado, int ticksDeEspera) {
        this.resultado = Map.copyOf(resultado);
        this.ticksRestantes = ticksDeEspera;
    }

    public boolean respondida() {
        return resultado != null;
    }

    /** Desconta 1 tick de espera; devolve true quando o barco pode seguir. */
    public boolean esperar() {
        if (ticksRestantes > 0) {
            ticksRestantes--;
            return false;
        }
        return true;
    }

    public void concluir() {
        concluida = true;
    }

    public boolean concluida() {
        return concluida;
    }

    public String barcoId() {
        return barcoId;
    }

    public CodigoGate gate() {
        return gate;
    }

    public List<Marca> marcas() {
        return marcas;
    }

    public Map<String, ResultadoMarca> resultado() {
        return resultado;
    }

    public int ticksRestantes() {
        return ticksRestantes;
    }
}
