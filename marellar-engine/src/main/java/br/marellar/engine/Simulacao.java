package br.marellar.engine;

import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dominio.Barco;
import br.marellar.engine.dominio.Evento;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.Pessoa;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.estado.EstadoPartida;
import br.marellar.engine.estado.EstadoPartida.BarcoVisao;
import br.marellar.engine.estado.EstadoPartida.PessoaVisao;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;
import br.marellar.engine.regras.InspecaoRegra;
import br.marellar.engine.regras.ProducaoRegra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fachada do motor: cria a partida, avanca o tempo, aplica ordens e devolve o estado.
 * <p>
 * Mesma semente + mesmos parametros + mesmas ordens nos mesmos ticks = mesmo estado.
 * Os metodos sao synchronized para a API poder atender varias requisicoes da mesma partida.
 */
public final class Simulacao {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final Partida partida;

    private Simulacao(Partida partida) {
        this.partida = partida;
    }

    public static Simulacao novaPartida(long semente) {
        return novaPartida(semente, CarregadorDados.carregarDoClasspath());
    }

    public static Simulacao novaPartida(long semente, ParametrosJogo parametros) {
        return novaPartida(semente, parametros, Partida.INICIO_PADRAO);
    }

    public static Simulacao novaPartida(long semente, ParametrosJogo parametros, LocalDate inicio) {
        return new Simulacao(new Partida(semente, parametros, inicio));
    }

    /**
     * Avanca ate {@code ticks} horas de turno. Para antes se o motor pausar sozinho
     * (fim do mes, e depois acidente, NC critica...). Chamar de novo continua a partida.
     */
    public synchronized ResultadoAvanco avancar(int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks nao pode ser negativo: " + ticks);
        }
        partida.limparPausa();
        int executados = 0;
        while (executados < ticks) {
            tick();
            executados++;
            if (partida.pausado()) {
                break;
            }
        }
        return new ResultadoAvanco(executados, partida.pausado(), partida.motivoPausa().orElse(null));
    }

    private void tick() {
        ProducaoRegra.executar(partida);
        InspecaoRegra.executar(partida);
        boolean ultimoTickDoMes = (partida.relogio().tick() + 1) % Relogio.TICKS_POR_MES == 0;
        if (ultimoTickDoMes) {
            // registrado ainda na ultima hora do mes, antes de o relogio virar
            partida.registrarEvento(TipoEvento.FIM_DO_MES, null, "eventos.fimDoMes", Map.of());
        }
        partida.relogio().avancar();
        if (ultimoTickDoMes) {
            partida.pausar(MotivoPausa.FIM_DO_MES);
        }
    }

    public synchronized void aplicarOrdem(Ordem ordem) {
        switch (ordem) {
            case Ordem.DefinirPrioridade o -> definirPrioridade(o.barcoId(), o.posicao());
            case Ordem.SegurarGate o -> barcoAtivo(o.barcoId()).definirRetido(true);
            case Ordem.LiberarGate o -> liberarGate(o.barcoId());
            case Ordem.MudarVelocidade o -> partida.definirVelocidade(o.velocidade());
        }
    }

    private void definirPrioridade(String barcoId, int posicao) {
        Barco alvo = barcoAtivo(barcoId);
        List<Barco> fila = new ArrayList<>(partida.barcosPorPrioridade());
        if (posicao < 1 || posicao > fila.size()) {
            throw new OrdemInvalidaException("Posicao fora da fila (1 a " + fila.size() + "): " + posicao);
        }
        fila.remove(alvo);
        fila.add(posicao - 1, alvo);
        for (int i = 0; i < fila.size(); i++) {
            fila.get(i).definirPrioridade(i + 1);
        }
    }

    private void liberarGate(String barcoId) {
        Barco barco = barcoAtivo(barcoId);
        barco.definirRetido(false);
        if (barco.status() == StatusBarco.APROVADO) {
            InspecaoRegra.seguirParaProximoGate(partida, barco);
        }
    }

    private Barco barcoAtivo(String barcoId) {
        Barco barco = partida.barco(barcoId)
                .orElseThrow(() -> new OrdemInvalidaException("Barco nao encontrado: " + barcoId));
        if (!barco.ativo()) {
            throw new OrdemInvalidaException("Barco ja entregue: " + barcoId);
        }
        return barco;
    }

    // ----- leitura -----

    public synchronized EstadoPartida estado() {
        Relogio r = partida.relogio();
        String mesRef = r.mesReferencia().getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "")
                + "/" + r.mesReferencia().getYear();
        return new EstadoPartida(
                partida.semente(),
                r.tick(),
                r.data(),
                r.hora(),
                r.mes() + 1,
                mesRef,
                r.diaUtilDoMes(),
                partida.velocidade(),
                partida.pausado(),
                partida.motivoPausa().orElse(null),
                partida.barcos().stream().map(this::visao).toList(),
                partida.pessoas().stream().map(Simulacao::visao).toList(),
                partida.barcoEmInspecao().orElse(null),
                partida.filaInspecao(),
                partida.horasOciosas(),
                partida.eventos().size());
    }

    public synchronized BarcoVisao barco(String barcoId) {
        return visao(partida.barco(barcoId)
                .orElseThrow(() -> new OrdemInvalidaException("Barco nao encontrado: " + barcoId)));
    }

    /** Eventos com id maior que {@code desdeId} (0 = todos). */
    public synchronized List<Evento> eventosDesde(long desdeId) {
        return partida.eventos().stream().filter(e -> e.id() > desdeId).toList();
    }

    private BarcoVisao visao(Barco b) {
        BigDecimal progresso = b.hhTotalDoGate().signum() == 0 ? BigDecimal.ONE
                : BigDecimal.ONE.subtract(b.hhRestantesNoGate().divide(b.hhTotalDoGate(), 4, RoundingMode.HALF_EVEN));
        List<String> alocadas = partida.pessoas().stream()
                .filter(p -> p.alocadoEm().map(b.id()::equals).orElse(false))
                .map(Pessoa::id)
                .toList();
        return new BarcoVisao(
                b.id(), b.linha(), b.cliente(), b.gateAtual(),
                partida.parametros().linha33().gate(b.gateAtual()).nome(),
                b.status(), b.retido(), b.prioridade(),
                b.hhTotalDoGate(), b.hhRestantesNoGate(), progresso,
                b.hhProduzidas(), b.hhApontadas(),
                List.copyOf(b.gatesAprovados()), alocadas);
    }

    private static PessoaVisao visao(Pessoa p) {
        return new PessoaVisao(p.id(), p.nome(), p.funcao(), p.nivel(), p.moral(), p.alocadoEm().orElse(null));
    }
}
