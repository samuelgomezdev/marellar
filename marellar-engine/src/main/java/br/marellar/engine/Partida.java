package br.marellar.engine;

import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dados.ParametrosLinha;
import br.marellar.engine.dados.ParametrosPessoas;
import br.marellar.engine.dominio.Barco;
import br.marellar.engine.dominio.Evento;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.NaoConformidade;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.Pessoa;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.dominio.Velocidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/**
 * Estado mutavel de uma partida. So o motor (Simulacao e regras) mexe aqui;
 * quem esta de fora le o estado pelo retrato imutavel {@link br.marellar.engine.estado.EstadoPartida}.
 */
public final class Partida {

    /** A partida do MVP comeca em marco de 2027 (1o de marco de 2027 e uma segunda-feira). */
    public static final LocalDate INICIO_PADRAO = LocalDate.of(2027, 3, 1);

    private static final String ALGORITMO_ALEATORIO = "L64X128MixRandom";

    private final long semente;
    private final ParametrosJogo parametros;
    private final RandomGenerator aleatorio;
    private final Relogio relogio;
    private final List<Barco> barcos = new ArrayList<>();
    private final List<Pessoa> pessoas = new ArrayList<>();
    private final Deque<String> filaInspecao = new ArrayDeque<>();
    private final List<Evento> eventos = new ArrayList<>();
    private String barcoEmInspecao;
    private int ticksRestantesInspecao;
    private Velocidade velocidade = Velocidade.PAUSA;
    private boolean pausado;
    private MotivoPausa motivoPausa;
    private BigDecimal horasOciosas = BigDecimal.ZERO;
    private final List<NaoConformidade> ncs = new ArrayList<>();
    private int contadorNc;
    private int concessoes;
    private int rncs;
    private BigDecimal custoMaterialRetrabalho = BigDecimal.ZERO;

    Partida(long semente, ParametrosJogo parametros, LocalDate inicio) {
        this.semente = semente;
        this.parametros = parametros;
        this.aleatorio = RandomGeneratorFactory.of(ALGORITMO_ALEATORIO).create(semente);
        this.relogio = new Relogio(inicio);

        ParametrosLinha linha = parametros.linha33();
        int prioridade = 1;
        for (ParametrosLinha.BarcoInicial b : linha.barcosIniciais()) {
            barcos.add(new Barco(b.id(), linha.linha(), b.cliente(), b.gateInicial(),
                    linha.hhDoGate(b.gateInicial()), prioridade++));
        }
        for (ParametrosPessoas.PessoaInicial p : parametros.pessoas().equipe()) {
            pessoas.add(new Pessoa(p.id(), p.nome(), p.funcao(), p.nivel(), p.moral(), p.personalidade()));
        }
    }

    // ----- eventos e pausa -----

    public Evento registrarEvento(TipoEvento tipo, String barcoId, String caminhoTexto, Map<String, String> valores) {
        String mensagem = parametros.textos().formatar(caminhoTexto, valores);
        Evento e = new Evento(eventos.size() + 1L, relogio.tick(), relogio.data(), relogio.hora(), tipo, barcoId, mensagem);
        eventos.add(e);
        return e;
    }

    public void pausar(MotivoPausa motivo) {
        this.pausado = true;
        this.motivoPausa = motivo;
    }

    void limparPausa() {
        this.pausado = false;
        this.motivoPausa = null;
    }

    // ----- barcos -----

    public Optional<Barco> barco(String id) {
        return barcos.stream().filter(b -> b.id().equals(id)).findFirst();
    }

    /** Barcos ainda nao entregues, do mais prioritario para o menos. */
    public List<Barco> barcosPorPrioridade() {
        return barcos.stream()
                .filter(Barco::ativo)
                .sorted(Comparator.comparingInt(Barco::prioridade).thenComparing(Barco::id))
                .toList();
    }

    // ----- qualidade -----

    public String proximoIdNc() {
        contadorNc++;
        return "NC-%04d".formatted(contadorNc);
    }

    public void adicionarNcs(List<NaoConformidade> novas) {
        ncs.addAll(novas);
    }

    public Optional<NaoConformidade> nc(String id) {
        return ncs.stream().filter(n -> n.id().equals(id)).findFirst();
    }

    public List<NaoConformidade> ncsDoBarco(String barcoId) {
        return ncs.stream().filter(n -> n.barcoId().equals(barcoId)).toList();
    }

    public List<NaoConformidade> ncsDoGateAtual(Barco barco) {
        return ncs.stream()
                .filter(n -> n.barcoId().equals(barco.id()) && n.gate() == barco.gateAtual())
                .toList();
    }

    public List<NaoConformidade> ncs() {
        return ncs;
    }

    public void registrarConcessao() {
        concessoes++;
    }

    public void registrarRnc() {
        rncs++;
    }

    public void somarCustoMaterialRetrabalho(BigDecimal valor) {
        custoMaterialRetrabalho = custoMaterialRetrabalho.add(valor);
    }

    public int concessoes() {
        return concessoes;
    }

    public int rncs() {
        return rncs;
    }

    public BigDecimal custoMaterialRetrabalho() {
        return custoMaterialRetrabalho;
    }

    public Pessoa inspetor() {
        return pessoas.stream().filter(p -> p.funcao() == Funcao.INSPETOR).findFirst()
                .orElseThrow(() -> new IllegalStateException("Partida sem inspetor"));
    }

    public Optional<String> fornecedorDoItem(String item) {
        if (item == null) {
            return Optional.empty();
        }
        return parametros.fornecedores().fornecedores().stream()
                .filter(f -> f.itens().contains(item))
                .map(f -> f.id())
                .findFirst();
    }

    public Optional<String> nomeDoFornecedor(String id) {
        return parametros.fornecedores().fornecedores().stream()
                .filter(f -> f.id().equals(id))
                .map(f -> f.nome())
                .findFirst();
    }

    // ----- inspecao -----

    public void entrarNaFilaDeInspecao(String barcoId) {
        filaInspecao.addLast(barcoId);
    }

    public Optional<String> proximoDaFila() {
        return Optional.ofNullable(filaInspecao.pollFirst());
    }

    public void iniciarInspecao(String barcoId, int ticks) {
        this.barcoEmInspecao = barcoId;
        this.ticksRestantesInspecao = ticks;
    }

    /** Desconta um tick da inspecao em andamento; devolve true quando ela termina. */
    public boolean trabalharInspecao() {
        ticksRestantesInspecao--;
        return ticksRestantesInspecao <= 0;
    }

    public void encerrarInspecao() {
        this.barcoEmInspecao = null;
        this.ticksRestantesInspecao = 0;
    }

    public void somarHorasOciosas(int horas) {
        horasOciosas = horasOciosas.add(BigDecimal.valueOf(horas));
    }

    void definirVelocidade(Velocidade v) {
        this.velocidade = v;
    }

    // ----- leitura -----

    public long semente() {
        return semente;
    }

    public ParametrosJogo parametros() {
        return parametros;
    }

    public RandomGenerator aleatorio() {
        return aleatorio;
    }

    public Relogio relogio() {
        return relogio;
    }

    public List<Barco> barcos() {
        return barcos;
    }

    public List<Pessoa> pessoas() {
        return pessoas;
    }

    public List<String> filaInspecao() {
        return List.copyOf(filaInspecao);
    }

    public Optional<String> barcoEmInspecao() {
        return Optional.ofNullable(barcoEmInspecao);
    }

    public List<Evento> eventos() {
        return eventos;
    }

    public Velocidade velocidade() {
        return velocidade;
    }

    public boolean pausado() {
        return pausado;
    }

    public Optional<MotivoPausa> motivoPausa() {
        return Optional.ofNullable(motivoPausa);
    }

    public BigDecimal horasOciosas() {
        return horasOciosas;
    }
}
