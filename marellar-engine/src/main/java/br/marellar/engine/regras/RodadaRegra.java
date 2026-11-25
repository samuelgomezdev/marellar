package br.marellar.engine.regras;

import br.marellar.engine.Aleatorio;
import br.marellar.engine.Partida;
import br.marellar.engine.dados.ParametrosRodada;
import br.marellar.engine.dominio.Barco;
import br.marellar.engine.dominio.GarantiaAgendada;
import br.marellar.engine.dominio.Marca;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.NaoConformidade;
import br.marellar.engine.dominio.ResultadoMarca;
import br.marellar.engine.dominio.RodadaInspecao;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.StatusNc;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.dominio.TipoLancamento;
import br.marellar.engine.ordens.OrdemInvalidaException;
import br.marellar.engine.relatorio.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rodada curta de inspecao 2D (sprint 2, etapa 3). Parametros em data/inspecao-rodada.json.
 * <ol>
 *   <li>Gate terminado: cada NC sorteada vira uma marca num ponto compativel; marcas falsas vao para pontos
 *       livres. Com {@code pausarNoGate}, o jogo pausa para o jogador inspecionar ou deixar com o inspetor.</li>
 *   <li>Ordem InspecionarRodada: marca de NC clicada = NC detectada; nao clicada = NC escapa; marca falsa
 *       clicada = falso alarme, o barco espera horasPorFalsoAlarme a mais. A rodada substitui o inspetor
 *       naquele gate. Sem rodada, vale a deteccao automatica do inspetor.</li>
 *   <li>Na entrega, cada NC que escapou (da rodada ou do inspetor) volta na garantia de garantiaDiasMin a
 *       garantiaDiasMax dias uteis depois, custando garantiaFatorCusto x (HH de retrabalho x custo da HH + material).</li>
 * </ol>
 * Os sorteios usam a sequencia propria da rodada (determinista com a semente).
 */
public final class RodadaRegra {

    private RodadaRegra() {
    }

    // ----- 1. gate terminado -----

    public static void preparar(Partida partida, Barco barco, List<NaoConformidade> ncs) {
        ParametrosRodada r = partida.parametros().rodada();
        Aleatorio sorteio = partida.aleatorioRodada();
        Map<String, String[]> porPonto = new LinkedHashMap<>();   // ponto -> {aparencia, ncId}
        for (NaoConformidade nc : ncs) {
            // tipo sem entrada no arquivo (so em cenarios de teste): qualquer ponto livre, aparencia generica
            ParametrosRodada.Tipo tipo = r.tipos().getOrDefault(nc.tipo(), new ParametrosRodada.Tipo("DEFEITO", List.of()));
            List<String> livres = new ArrayList<>(tipo.pontos());
            livres.removeAll(porPonto.keySet());
            if (livres.isEmpty()) {
                livres = pontosLivres(r, porPonto);
            }
            porPonto.put(livres.get(sorteio.proximoInt(livres.size())), new String[] {tipo.aparencia(), nc.id()});
        }
        int falsas = sorteio.proximoInt(r.marcasFalsasMin(), r.marcasFalsasMax() + 1);
        for (int i = 0; i < falsas; i++) {
            List<String> livres = pontosLivres(r, porPonto);
            if (livres.isEmpty()) {
                break;
            }
            String ponto = livres.get(sorteio.proximoInt(livres.size()));
            String aparencia = r.aparenciasFalsas().get(sorteio.proximoInt(r.aparenciasFalsas().size()));
            porPonto.put(ponto, new String[] {aparencia, null});
        }
        // marcas na ordem dos pontos: a ordem nao diz qual e NC
        List<Marca> marcas = new ArrayList<>();
        for (ParametrosRodada.Ponto p : r.pontos()) {
            String[] m = porPonto.get(p.id());
            if (m != null) {
                marcas.add(new Marca("M" + (marcas.size() + 1), p.id(), m[0], m[1]));
            }
        }
        partida.rodadas().put(barco.id(), new RodadaInspecao(barco.id(), barco.gateAtual(), marcas));
        if (r.pausarNoGate()) {
            partida.pausar(MotivoPausa.RODADA_INSPECAO, barco.id());
        }
    }

    private static List<String> pontosLivres(ParametrosRodada r, Map<String, String[]> ocupados) {
        List<String> livres = new ArrayList<>();
        for (ParametrosRodada.Ponto p : r.pontos()) {
            if (!ocupados.containsKey(p.id())) {
                livres.add(p.id());
            }
        }
        return livres;
    }

    /** O inspetor terminou a inspecao automatica: a rodada daquele gate deixa de valer. */
    static void descartar(Partida partida, Barco barco) {
        RodadaInspecao rodada = partida.rodadas().get(barco.id());
        if (rodada != null && !rodada.respondida()) {
            partida.rodadas().remove(barco.id());
        }
    }

    // ----- 2. resposta do jogador -----

    public static void inspecionar(Partida partida, String barcoId, List<String> marcasClicadas) {
        RodadaInspecao rodada = partida.rodadas().get(barcoId);
        if (rodada == null || rodada.respondida()) {
            throw new OrdemInvalidaException("Nenhuma rodada de inspecao aberta para " + barcoId);
        }
        Barco barco = partida.barco(barcoId).orElseThrow();
        if (barco.gateAtual() != rodada.gate()
                || (barco.status() != StatusBarco.AGUARDANDO_INSPECAO && barco.status() != StatusBarco.EM_INSPECAO)) {
            throw new OrdemInvalidaException("O barco " + barcoId + " nao esta esperando inspecao");
        }
        Set<String> clicadas = new HashSet<>(marcasClicadas == null ? List.of() : marcasClicadas);
        Set<String> ids = new HashSet<>();
        rodada.marcas().forEach(m -> ids.add(m.id()));
        if (!ids.containsAll(clicadas)) {
            throw new OrdemInvalidaException("Marca inexistente na rodada de " + barcoId + ": " + clicadas);
        }

        Map<String, ResultadoMarca> resultado = new LinkedHashMap<>();
        Map<String, Boolean> ncClicada = new LinkedHashMap<>();
        int achadas = 0;
        int escaparam = 0;
        int falsos = 0;
        for (Marca m : rodada.marcas()) {
            boolean clicou = clicadas.contains(m.id());
            ResultadoMarca r;
            if (m.falsa()) {
                r = clicou ? ResultadoMarca.FALSO_ALARME : ResultadoMarca.IGNOROU;
                falsos += clicou ? 1 : 0;
            } else {
                r = clicou ? ResultadoMarca.ACHOU : ResultadoMarca.ESCAPOU;
                ncClicada.put(m.ncId(), clicou);
                achadas += clicou ? 1 : 0;
                escaparam += clicou ? 0 : 1;
            }
            resultado.put(m.id(), r);
        }
        for (NaoConformidade nc : partida.ncsDoGateAtual(barco)) {
            if (nc.status() != StatusNc.PENDENTE_INSPECAO) {
                continue;
            }
            if (Boolean.TRUE.equals(ncClicada.get(nc.id()))) {
                nc.detectar();
                partida.mes().contarNcDetectada();
            } else {
                nc.escapar();
                partida.mes().contarNcEscapada();
            }
        }
        // a rodada toma o lugar do inspetor neste gate
        if (partida.barcoEmInspecao().map(barcoId::equals).orElse(false)) {
            partida.encerrarInspecao();
        } else {
            partida.sairDaFilaDeInspecao(barcoId);
        }
        barco.mudarStatus(StatusBarco.EM_INSPECAO);
        rodada.responder(resultado, falsos * partida.parametros().rodada().horasPorFalsoAlarme());
        partida.registrarUltimaRodada(rodada);
        partida.registrarEvento(TipoEvento.RODADA, barco.id(), "eventos.rodada", Map.of("barco", barco.id(),
                "gate", rodada.gate().name(), "achadas", String.valueOf(achadas),
                "escaparam", String.valueOf(escaparam), "falsos", String.valueOf(falsos)));
    }

    // ----- a cada tick: o barco da rodada respondida segue depois da espera -----

    public static void executar(Partida partida) {
        Iterator<RodadaInspecao> it = partida.rodadas().values().iterator();
        List<RodadaInspecao> prontas = new ArrayList<>();
        while (it.hasNext()) {
            RodadaInspecao rodada = it.next();
            if (rodada.respondida() && rodada.esperar()) {
                it.remove();
                prontas.add(rodada);
            }
        }
        for (RodadaInspecao rodada : prontas) {
            rodada.concluir();
            Barco barco = partida.barco(rodada.barcoId()).orElseThrow();
            List<NaoConformidade> detectadas = partida.ncsDoGateAtual(barco).stream()
                    .filter(nc -> nc.status() == StatusNc.ABERTA).toList();
            QualidadeRegra.concluirInspecao(partida, barco, detectadas);
        }
    }

    // ----- 3. garantia -----

    /** Barco entregue: cada NC que escapou volta no pos-venda num dia sorteado. */
    public static void agendarGarantias(Partida partida, Barco barco) {
        ParametrosRodada r = partida.parametros().rodada();
        for (NaoConformidade nc : partida.ncsDoBarco(barco.id())) {
            if (nc.status() != StatusNc.ESCAPADA) {
                continue;
            }
            long dia = partida.hoje() + partida.aleatorioRodada().proximoInt(r.garantiaDiasMin(), r.garantiaDiasMax() + 1);
            partida.garantias().add(new GarantiaAgendada(barco.id(), nc.id(), nc.descricao(), dia, custoGarantia(partida, nc)));
        }
    }

    /** Garantia que venceu hoje sai do caixa e entra na DRE do mes. */
    public static void fimDoDia(Partida partida) {
        Iterator<GarantiaAgendada> it = partida.garantias().iterator();
        while (it.hasNext()) {
            GarantiaAgendada g = it.next();
            if (g.dia() > partida.hoje()) {
                continue;
            }
            it.remove();
            partida.lancar(TipoLancamento.GARANTIA, g.custo().negate(), "Garantia de " + g.barcoId() + ": " + g.descricao());
            partida.mes().somarGarantia(g.custo());
            partida.registrarEvento(TipoEvento.GARANTIA, g.barcoId(), "eventos.garantia", Map.of("barco", g.barcoId(),
                    "descricao", g.descricao(), "valor", Dinheiro.formatar(g.custo()),
                    "fator", partida.parametros().rodada().garantiaFatorCusto().stripTrailingZeros().toPlainString()));
        }
    }

    /** garantiaFatorCusto x (HH de retrabalho x custo da HH + material). */
    public static BigDecimal custoGarantia(Partida partida, NaoConformidade nc) {
        BigDecimal naFabrica = nc.hhRetrabalho().multiply(partida.parametros().linha33().custoHh()).add(nc.custoMaterial());
        return naFabrica.multiply(partida.parametros().rodada().garantiaFatorCusto()).setScale(2, RoundingMode.HALF_EVEN);
    }
}
