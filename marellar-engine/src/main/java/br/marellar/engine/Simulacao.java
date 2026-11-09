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
import br.marellar.engine.estado.EstadoPartida.CotaVisao;
import br.marellar.engine.estado.EstadoPartida.SituacaoCota;
import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.estado.EstadoPartida.PessoaVisao;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;
import br.marellar.engine.regras.InspecaoRegra;
import br.marellar.engine.regras.ProducaoRegra;
import br.marellar.engine.regras.QualidadeRegra;
import br.marellar.engine.regras.EventoRegra;
import br.marellar.engine.regras.FornecedorRegra;
import br.marellar.engine.regras.PessoasRegra;
import br.marellar.engine.regras.ConselhoRegra;
import br.marellar.engine.regras.FinanceiroRegra;
import br.marellar.engine.estado.EstadoPartida.FinanceiroVisao;
import br.marellar.engine.relatorio.RelatorioConselho;
import br.marellar.engine.dominio.CotaAReceber;
import br.marellar.engine.dominio.Fornecedor;
import br.marellar.engine.dominio.Pedido;
import br.marellar.engine.dominio.Rnc;
import br.marellar.engine.estado.EstadoPartida.FornecedorVisao;
import br.marellar.engine.estado.EstadoPartida.PedidoVisao;
import br.marellar.engine.estado.EstadoPartida.RncVisao;
import br.marellar.engine.estado.EstadoPartida.SegurancaVisao;
import br.marellar.engine.dominio.NaoConformidade;
import br.marellar.engine.dominio.StatusNc;
import br.marellar.engine.estado.EstadoPartida.InspetorVisao;
import br.marellar.engine.estado.EstadoPartida.NcVisao;
import br.marellar.engine.estado.EstadoPartida.QualidadeVisao;
import br.marellar.engine.relatorio.IndicadoresQualidade;
import br.marellar.engine.save.Save;
import br.marellar.engine.save.SaveIncompativelException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fachada do motor: cria a partida, avanca o tempo, aplica ordens e devolve o estado.
 * <p>
 * Mesma semente + mesmos parametros + mesmas ordens nos mesmos ticks = mesmo estado.
 * Os metodos sao synchronized para a API poder atender varias requisicoes da mesma partida.
 */
public final class Simulacao {


    private final Partida partida;
    private final LocalDate inicio;
    /** Ordens aceitas, com o tick em que entraram: e o que vai para o save. */
    private final List<Save.OrdemNoTempo> historico = new ArrayList<>();

    private Simulacao(Partida partida, LocalDate inicio) {
        this.partida = partida;
        this.inicio = inicio;
    }

    public static Simulacao novaPartida(long semente) {
        return novaPartida(semente, CarregadorDados.carregarDoClasspath());
    }

    public static Simulacao novaPartida(long semente, ParametrosJogo parametros) {
        return novaPartida(semente, parametros, Partida.INICIO_PADRAO);
    }

    public static Simulacao novaPartida(long semente, ParametrosJogo parametros, LocalDate inicio) {
        return new Simulacao(new Partida(semente, parametros, inicio), inicio);
    }

    // ----- salvar e carregar -----

    /** Retrato da partida para gravar em disco (ver {@link Save}). */
    public synchronized Save salvar() {
        Relogio r = partida.relogio();
        return new Save(Save.VERSAO_ATUAL, partida.semente(), inicio.toString(), r.tick(), historico,
                new Save.Verificacao(r.data().toString(), r.hora(), partida.caixa(), partida.eventos().size(),
                        assinatura()));
    }

    /** SHA-256 de barcos, pessoas, eventos e caixa (records sem Map: o texto e estavel entre execucoes). */
    private String assinatura() {
        StringBuilder texto = new StringBuilder();
        partida.barcos().forEach(b -> texto.append(visao(b)).append('\n'));
        partida.pessoas().forEach(p -> texto.append(visao(p)).append('\n'));
        partida.eventos().forEach(e -> texto.append(e).append('\n'));
        texto.append(partida.caixa().toPlainString());
        return br.marellar.engine.save.Sha256.hex(texto.toString());
    }

    /**
     * Recria a partida jogando de novo: avanca ate o tick de cada ordem, aplica a ordem e segue
     * ate o tick final. Se o resultado nao bater com a verificacao do save, os parametros mudaram.
     */
    public static Simulacao carregar(Save save, ParametrosJogo parametros) {
        if (save.versao() != Save.VERSAO_ATUAL) {
            throw new SaveIncompativelException("Versao de save desconhecida: " + save.versao());
        }
        Simulacao sim = novaPartida(save.semente(), parametros, LocalDate.parse(save.inicio()));
        try {
            for (Save.OrdemNoTempo o : save.ordens()) {
                sim.avancarAte(o.tick());
                sim.aplicarOrdem(o.ordem());
            }
            sim.avancarAte(save.tick());
        } catch (OrdemInvalidaException e) {
            throw new SaveIncompativelException("O save nao reproduz com os parametros atuais: " + e.getMessage(), e);
        }
        Save.Verificacao esperado = save.verificacao();
        Save.Verificacao obtido = sim.salvar().verificacao();
        if (esperado == null || !obtido.assinatura().equals(esperado.assinatura())
                || obtido.caixa().compareTo(esperado.caixa()) != 0 || !obtido.data().equals(esperado.data())
                || obtido.hora() != esperado.hora() || obtido.eventos() != esperado.eventos()) {
            throw new SaveIncompativelException("O save foi feito com outros parametros de data/ (ou foi alterado): "
                    + "esperado " + esperado + ", obtido " + obtido);
        }
        return sim;
    }

    /** Avanca, atravessando as pausas automaticas, ate o relogio chegar ao tick pedido. */
    private void avancarAte(long tickAlvo) {
        while (partida.relogio().tick() < tickAlvo) {
            long faltam = tickAlvo - partida.relogio().tick();
            ResultadoAvanco r = avancar((int) Math.min(faltam, Integer.MAX_VALUE));
            if (r.ticksExecutados() == 0) {
                throw new SaveIncompativelException("A partida acabou antes do tick " + tickAlvo + " do save");
            }
        }
    }

    /**
     * Avanca ate {@code ticks} horas de turno. Para antes se o motor pausar sozinho
     * (fim do mes, e depois acidente, NC critica...). Chamar de novo continua a partida.
     */
    public synchronized ResultadoAvanco avancar(int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks nao pode ser negativo: " + ticks);
        }
        if (partida.derrota().isPresent()) {
            return new ResultadoAvanco(0, true, MotivoPausa.DERROTA);
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
        if (partida.relogio().horaDoTurno() == Relogio.TICKS_POR_DIA - 1) {
            // rotinas diarias no fim do turno: o que acontece hoje vale a partir de amanha
            PessoasRegra.fimDoDia(partida);
            FornecedorRegra.fimDoDia(partida);
            FinanceiroRegra.fimDoDia(partida);
        }
        boolean ultimoTickDoMes = (partida.relogio().tick() + 1) % Relogio.TICKS_POR_MES == 0;
        if (ultimoTickDoMes) {
            // registrado ainda na ultima hora do mes, antes de o relogio virar
            FornecedorRegra.scorecard(partida);
            ConselhoRegra.reunir(partida, FinanceiroRegra.fecharMes(partida));
            partida.registrarEvento(TipoEvento.FIM_DO_MES, null, "eventos.fimDoMes", Map.of());
        }
        partida.relogio().avancar();
        if (ultimoTickDoMes) {
            partida.pausar(MotivoPausa.FIM_DO_MES);
            ConselhoRegra.pausarSeDerrota(partida);
        }
    }

    public synchronized void aplicarOrdem(Ordem ordem) {
        if (partida.derrota().isPresent() && !(ordem instanceof Ordem.MudarVelocidade)) {
            throw new OrdemInvalidaException("A partida acabou: " + partida.derrota().get());
        }
        switch (ordem) {
            case Ordem.DefinirPrioridade o -> definirPrioridade(o.barcoId(), o.posicao());
            case Ordem.SegurarGate o -> barcoAtivo(o.barcoId()).definirRetido(true);
            case Ordem.LiberarGate o -> liberarGate(o.barcoId());
            case Ordem.DecidirNc o -> QualidadeRegra.decidir(partida, o.ncId(), o.decisao());
            case Ordem.Contratar o -> PessoasRegra.contratar(partida, o.funcao());
            case Ordem.Treinar o -> PessoasRegra.treinar(partida, o.pessoaId(), o.treinamento());
            case Ordem.FazerUmAUm o -> PessoasRegra.fazerUmAUm(partida, o.pessoaId());
            case Ordem.EscolherDecisaoConselho o -> ConselhoRegra.escolher(partida, o.opcaoId());
            case Ordem.MudarVelocidade o -> partida.definirVelocidade(o.velocidade());
        }
        historico.add(new Save.OrdemNoTempo(partida.relogio().tick(), ordem));
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
        String mesRef = r.rotuloMesReferencia();
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
                partida.pessoas().stream().map(this::visao).toList(),
                inspetorVisao(),
                partida.barcoEmInspecao().orElse(null),
                partida.filaInspecao(),
                qualidadeVisao(),
                partida.fornecedores().stream().map(this::visao).toList(),
                Map.copyOf(partida.estoque()),
                partida.pedidos().stream().filter(p -> p.diaChegada() > partida.hoje()).map(this::visao).toList(),
                partida.rncs().stream().map(this::visao).toList(),
                segurancaVisao(),
                financeiroVisao(),
                partida.ultimoConselho().orElse(null),
                partida.derrota().orElse(null),
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
        var linha = partida.parametros().linha33();
        List<CotaVisao> cotas = cotasDoBarco(b);
        return new BarcoVisao(
                b.id(), b.linha(), b.cliente(), b.gateAtual(),
                partida.parametros().linha33().gate(b.gateAtual()).nome(),
                b.status(), b.retido(), b.prioridade(),
                b.hhTotalDoGate(), b.hhRestantesNoGate(), progresso, b.hhRetrabalhoPendente(), b.hhQuebraPendente(),
                b.hhProduzidas(), b.hhRetrabalho(), b.hhApontadas(), b.horasParadoMaterial(),
                List.copyOf(b.gatesAprovados()), alocadas,
                partida.ncsDoBarco(b.id()).stream().filter(NaoConformidade::visivel).map(Simulacao::visao).toList(),
                linha.hhPorBarco(), hhConcluidas(b), linha.materialPorBarco(), materialAplicado(b),
                linha.precoVenda(), cotas.stream().filter(c -> c.situacao() == SituacaoCota.PAGA)
                        .map(CotaVisao::valor).reduce(BigDecimal.ZERO, BigDecimal::add),
                cotas);
    }

    /** HH padrao dos gates aprovados + o que ja foi feito no gate atual. */
    private BigDecimal hhConcluidas(Barco b) {
        var linha = partida.parametros().linha33();
        BigDecimal soma = b.gatesAprovados().stream().map(linha::hhDoGate).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!b.gatesAprovados().contains(b.gateAtual())) {
            soma = soma.add(linha.hhDoGate(b.gateAtual()).subtract(b.hhRestantesNoGate()).max(BigDecimal.ZERO)
                    .min(linha.hhDoGate(b.gateAtual())));
        }
        return soma.setScale(0, RoundingMode.HALF_EVEN);
    }

    /** Kits (a preco de tabela) dos gates aprovados e do gate atual, se ele ja comecou com material. */
    private BigDecimal materialAplicado(Barco b) {
        var linha = partida.parametros().linha33();
        java.util.Set<CodigoGate> gates = new java.util.LinkedHashSet<>(b.gatesAprovados());
        if (b.status() != StatusBarco.PARADO_MATERIAL) {
            gates.add(b.gateAtual());
        }
        return gates.stream().map(linha::itemDoGate).flatMap(java.util.Optional::stream)
                .map(linha::valorDoKit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Contrato (sempre pago) e uma cota por gate com cotaPercentual > 0. */
    private List<CotaVisao> cotasDoBarco(Barco b) {
        var linha = partida.parametros().linha33();
        var textos = partida.parametros().textos();
        List<CotaVisao> lista = new ArrayList<>();
        lista.add(new CotaVisao(null, textos.formatar("cotas.contrato", Map.of()), linha.cotaContratoPercentual(),
                FinanceiroRegra.percentual(linha.precoVenda(), linha.cotaContratoPercentual()), SituacaoCota.PAGA, null));
        for (var g : linha.gates()) {
            if (g.cotaPercentual().signum() <= 0) {
                continue;
            }
            BigDecimal valor = FinanceiroRegra.percentual(linha.precoVenda(), g.cotaPercentual());
            var aReceber = partida.cotasAReceber().stream()
                    .filter(c -> c.barcoId().equals(b.id()) && c.gate() == g.codigo()).findFirst();
            SituacaoCota situacao = aReceber.isPresent() ? SituacaoCota.A_RECEBER
                    : b.gatesAprovados().contains(g.codigo()) ? SituacaoCota.PAGA : SituacaoCota.PENDENTE;
            lista.add(new CotaVisao(g.codigo(), g.nome(), g.cotaPercentual(), valor, situacao,
                    aReceber.map(CotaAReceber::diaRecebimento).orElse(null)));
        }
        return lista;
    }

    private static NcVisao visao(NaoConformidade nc) {
        return new NcVisao(nc.id(), nc.gate(), nc.tipo(), nc.descricao(), nc.gravidade(), nc.origem(),
                nc.fornecedorId().orElse(null), nc.status(), nc.decisao().orElse(null),
                nc.hhRetrabalho(), nc.custoMaterial());
    }

    private InspetorVisao inspetorVisao() {
        Pessoa i = partida.inspetor();
        var p = i.personalidade().orElseThrow();
        return new InspetorVisao(i.id(), i.nome(), i.moral(), p.rigor(), p.velocidade(), p.paciencia());
    }

    private QualidadeVisao qualidadeVisao() {
        IndicadoresQualidade q = calcularIndicadores();
        return new QualidadeVisao(q.ncsDetectadas(), q.ncsAbertas(), q.concessoes(), q.rncs(),
                q.hhRetrabalho(), q.custoMaterialRetrabalho());
    }

    /** Indicadores completos, inclusive as NCs que escaparam (o jogador nao ve estas). */
    public synchronized IndicadoresQualidade indicadoresQualidade() {
        return calcularIndicadores();
    }

    private IndicadoresQualidade calcularIndicadores() {
        List<NaoConformidade> ncs = partida.ncs();
        int escapadas = (int) ncs.stream().filter(n -> n.status() == StatusNc.ESCAPADA).count();
        int pendentesInspecao = (int) ncs.stream().filter(n -> n.status() == StatusNc.PENDENTE_INSPECAO).count();
        int abertas = (int) ncs.stream().filter(n -> n.status() == StatusNc.ABERTA).count();
        BigDecimal hhRetrabalho = partida.barcos().stream().map(Barco::hhRetrabalho)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new IndicadoresQualidade(ncs.size(), ncs.size() - escapadas - pendentesInspecao, escapadas, abertas,
                partida.concessoes(), partida.quantidadeRncs(), hhRetrabalho, partida.custoMaterialRetrabalho());
    }

    private PessoaVisao visao(Pessoa p) {
        return new PessoaVisao(p.id(), p.nome(), p.funcao(), p.nivel(), p.moral(), p.situacao(),
                p.diasRestantesSituacao(), p.treinamentoEmCurso().orElse(null), p.treinoSegurancaEmDia(),
                p.contratadaNaPartida(),
                p.podeFazerUmAUm(partida.hoje(), partida.parametros().pessoas().diasEntreUmAUm()),
                p.alocadoEm().orElse(null));
    }

    private FornecedorVisao visao(Fornecedor f) {
        int abertas = (int) partida.rncs().stream()
                .filter(r -> r.fornecedorId().equals(f.id()) && r.aguardandoResposta()).count();
        return new FornecedorVisao(f.id(), f.nome(), f.itens(), f.nota(), f.pontos(), f.ppm(),
                f.pedidosEntregues(), f.pedidosNoPrazo(), f.oitoDsRespondidos(), f.oitoDsNoPrazo(),
                f.credito(), f.totalChargeback(), abertas);
    }

    private PedidoVisao visao(Pedido p) {
        String fornecedor = partida.fornecedor(p.fornecedorId()).map(Fornecedor::nome).orElse(p.fornecedorId());
        return new PedidoVisao(p.id(), fornecedor, p.item(), p.kits(), dataDoDia(p.diaPrevisto()), p.valorPago());
    }

    private RncVisao visao(Rnc r) {
        return new RncVisao(r.id(), r.ncId(), r.barcoId(), r.fornecedorId(), r.gravidade(), r.custoDocumentado(),
                r.chargeback(), dataDoDia(r.prazo8d()), r.status(), r.chargebackHorasParadas());
    }

    private SegurancaVisao segurancaVisao() {
        return new SegurancaVisao(partida.acidentes(), partida.diasPerdidos(), partida.horasTrabalhadas(),
                EventoRegra.taxaFrequencia(partida.acidentes(), partida.horasTrabalhadas()),
                EventoRegra.taxaGravidade(partida.diasPerdidos(), partida.horasTrabalhadas()),
                partida.demissoes(), partida.horasLiderUmAUm());
    }

    private FinanceiroVisao financeiroVisao() {
        BigDecimal aReceber = partida.cotasAReceber().stream().map(CotaAReceber::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        int n = partida.lancamentos().size();
        return new FinanceiroVisao(partida.caixa(), aReceber, partida.valorEmpresa(), partida.pacienciaConselho(),
                partida.parametros().financeiro().conselho().pacienciaMaxima(), partida.mesesCaixaNegativo(),
                partida.mes().receita(), List.copyOf(partida.lancamentos().subList(Math.max(0, n - 10), n)));
    }

    /** Todos os relatorios do conselho da partida, do primeiro ao ultimo. */
    public synchronized List<RelatorioConselho> conselhos() {
        return List.copyOf(partida.conselhos());
    }

    /** Todos os movimentos de caixa da partida. */
    public synchronized List<br.marellar.engine.dominio.Lancamento> lancamentos() {
        return List.copyOf(partida.lancamentos());
    }

    /** Ultimo relatorio do conselho (vazio antes do primeiro fim de mes). */
    public synchronized java.util.Optional<RelatorioConselho> conselho() {
        return partida.ultimoConselho();
    }

    private LocalDate dataDoDia(long diaUtil) {
        return Relogio.somarDiasUteis(partida.relogio().inicio(), diaUtil);
    }
}
