package br.marellar.engine.dados;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Rodada curta de inspecao 2D (arquivo data/inspecao-rodada.json; sprint 2, etapa 3).
 * <ul>
 *   <li>Ao terminar um gate, cada NC sorteada vira uma marca num ponto compativel do barco (vista de cima),
 *       e de marcasFalsasMin a marcasFalsasMax marcas falsas (sujeira, reflexo, fita) vao para pontos livres.</li>
 *   <li>{@code pausarNoGate}: o jogo pausa para o jogador inspecionar ou deixar com o inspetor.</li>
 *   <li>{@code segundos}: tempo da rodada na tela (o cliente cronometra; o motor so recebe os cliques).</li>
 *   <li>Marca falsa clicada: o barco espera horasPorFalsoAlarme a mais.</li>
 *   <li>NC que escapa (da rodada ou do inspetor) volta na garantia de garantiaDiasMin a garantiaDiasMax dias uteis
 *       depois da entrega, custando garantiaFatorCusto x (HH de retrabalho x custo da HH + material).</li>
 * </ul>
 * x e y dos pontos vao de 0 a 1 na vista de cima com a proa para cima (x = boreste para a direita).
 */
public record ParametrosRodada(
        boolean pausarNoGate,
        int segundos,
        int horasPorFalsoAlarme,
        int marcasFalsasMin,
        int marcasFalsasMax,
        List<String> aparenciasFalsas,
        BigDecimal garantiaFatorCusto,
        int garantiaDiasMin,
        int garantiaDiasMax,
        List<Ponto> pontos,
        Map<String, Tipo> tipos) {

    public ParametrosRodada {
        aparenciasFalsas = aparenciasFalsas == null ? List.of() : List.copyOf(aparenciasFalsas);
        pontos = pontos == null ? List.of() : List.copyOf(pontos);
        tipos = tipos == null ? Map.of() : Map.copyOf(tipos);
    }

    public record Ponto(String id, String nome, BigDecimal x, BigDecimal y) {}

    /** Como o defeito aparece na vista de cima e em que pontos ele pode estar. */
    public record Tipo(String aparencia, List<String> pontos) {
        public Tipo {
            pontos = pontos == null ? List.of() : List.copyOf(pontos);
        }
    }

    public ParametrosRodada comPausa(boolean pausar) {
        return new ParametrosRodada(pausar, segundos, horasPorFalsoAlarme, marcasFalsasMin, marcasFalsasMax,
                aparenciasFalsas, garantiaFatorCusto, garantiaDiasMin, garantiaDiasMax, pontos, tipos);
    }
}
