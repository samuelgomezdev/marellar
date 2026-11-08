package br.marellar.cli;

import br.marellar.engine.Relogio;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.DadosInvalidosException;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dominio.DecisaoNc;
import br.marellar.engine.dominio.Evento;
import br.marellar.engine.dominio.Origem;
import br.marellar.engine.dominio.SituacaoPessoa;
import br.marellar.engine.dominio.StatusNc;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.estado.EstadoPartida;
import br.marellar.engine.estado.EstadoPartida.BarcoVisao;
import br.marellar.engine.relatorio.IndicadoresQualidade;
import br.marellar.engine.relatorio.Dinheiro;
import br.marellar.engine.relatorio.Dre;
import br.marellar.engine.relatorio.RelatorioConselho;

import java.io.PrintStream;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Roda uma partida inteira pelo terminal e imprime o resumo.
 * <pre>
 * java -jar marellar-cli.jar --semente 42 --meses 1 [--dados caminho/para/data] [--eventos] [--sem-decisoes]
 * </pre>
 * Por padrao, um jogador automatico manda toda NC detectada para retrabalho.
 * O relatorio completo do conselho entra no M5/M6; por enquanto sai o resumo da producao.
 */
public final class Main {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Main() {
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        Argumentos a;
        try {
            a = Argumentos.ler(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Uso: java -jar marellar-cli.jar --semente 42 --meses 1 [--dados pasta] [--eventos] [--sem-decisoes]");
            System.exit(2);
            return;
        }

        ParametrosJogo parametros;
        try {
            parametros = a.dados() == null
                    ? CarregadorDados.carregarDoClasspath()
                    : CarregadorDados.carregarDeDiretorio(a.dados());
        } catch (DadosInvalidosException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        Simulacao sim = Simulacao.novaPartida(a.semente(), parametros);
        long alvo = (long) a.meses() * Relogio.TICKS_POR_MES;
        long rodados = 0;
        while (rodados < alvo) {
            // de hora em hora, para o jogador automatico decidir as NCs assim que aparecem
            rodados += sim.avancar(1).ticksExecutados();
            if (a.decidirNcs()) {
                decidirTudoComoRetrabalho(sim);
                umAUmComQuemEstaDesanimado(sim);
            }
        }

        imprimir(out, sim, a);
    }

    /** Jogador automatico da CLI: NC de material vira RNC contra o fornecedor; as outras, retrabalho. */
    static void decidirTudoComoRetrabalho(Simulacao sim) {
        for (BarcoVisao b : sim.estado().barcos()) {
            for (EstadoPartida.NcVisao nc : b.ncs()) {
                if (nc.status() == StatusNc.ABERTA) {
                    DecisaoNc decisao = nc.origem() == Origem.FORNECEDOR ? DecisaoNc.RNC_FORNECEDOR : DecisaoNc.RETRABALHO;
                    sim.aplicarOrdem(new Ordem.DecidirNc(nc.id(), decisao));
                }
            }
        }
    }

    /** Jogador automatico: no inicio do dia, 1:1 com quem esta com moral abaixo de 70 e pode ter 1:1. */
    static void umAUmComQuemEstaDesanimado(Simulacao sim) {
        EstadoPartida e = sim.estado();
        if (e.hora() != 7) {
            return;
        }
        for (EstadoPartida.PessoaVisao p : e.pessoas()) {
            boolean naEmpresa = p.situacao() != SituacaoPessoa.DESLIGADA && p.situacao() != SituacaoPessoa.A_CAMINHO;
            if (naEmpresa && p.podeFazerUmAUm() && p.moral() < 70) {
                sim.aplicarOrdem(new Ordem.FazerUmAUm(p.id()));
            }
        }
    }

    static void imprimir(PrintStream out, Simulacao sim, Argumentos a) {
        EstadoPartida e = sim.estado();
        List<Evento> eventos = sim.eventosDesde(0);

        out.println("==============================================================");
        out.printf(" MARELLAR  |  semente %d  |  %d mes(es)  |  NCs: %s%n", a.semente(), a.meses(),
                a.decidirNcs() ? "decisão automática" : "sem decisões");
        out.printf(" %d tick(s) jogados; o relógio está em %s, início de %s%n",
                e.tick(), DATA.format(e.data()), e.mesReferencia());
        out.println("==============================================================");
        out.println();
        out.println(" Barcos da linha de 33 pés");
        out.println(" ------------------------------------------------------------");
        out.printf(" %-7s %-4s %-20s %9s %9s %s%n", "Barco", "Gate", "Status", "HH padrão", "HH pagas", "Gates aprovados");
        for (BarcoVisao b : e.barcos()) {
            out.printf(" %-7s %-4s %-20s %9s %9s %s%n",
                    b.id(), b.gateAtual(), b.status(),
                    b.hhProduzidas().setScale(0, RoundingMode.HALF_EVEN).toPlainString(),
                    b.hhApontadas().setScale(0, RoundingMode.HALF_EVEN).toPlainString(),
                    b.gatesAprovados().stream().map(Enum::name).collect(Collectors.joining(" ")));
        }
        out.println();
        out.printf(" Horas ociosas da equipe: %s h%n", e.horasOciosas().toPlainString());

        IndicadoresQualidade q = sim.indicadoresQualidade();
        out.println();
        out.println(" Qualidade");
        out.println(" ------------------------------------------------------------");
        out.printf(" NCs geradas: %d | detectadas: %d | escapadas (o jogador não vê): %d | abertas: %d%n",
                q.ncsGeradas(), q.ncsDetectadas(), q.ncsEscapadas(), q.ncsAbertas());
        out.printf(" Concessões: %d | RNCs: %d | HH de retrabalho: %s | material de retrabalho: R$ %s%n",
                q.concessoes(), q.rncs(), q.hhRetrabalho().setScale(0, RoundingMode.HALF_EVEN).toPlainString(),
                q.custoMaterialRetrabalho().toPlainString());
        out.printf(" Inspetora %s: moral %d%n", e.inspetor().nome(), e.inspetor().moral());

        out.println();
        out.println(" Pessoas e segurança");
        out.println(" ------------------------------------------------------------");
        Map<SituacaoPessoa, Long> situacoes = e.pessoas().stream()
                .collect(Collectors.groupingBy(EstadoPartida.PessoaVisao::situacao, TreeMap::new, Collectors.counting()));
        out.println(" Situação: " + situacoes);
        out.printf(" Moral média da produção: %d%n", (int) e.pessoas().stream()
                .filter(p -> p.situacao() != SituacaoPessoa.DESLIGADA && p.funcao() == br.marellar.engine.dominio.Funcao.PRODUCAO)
                .mapToInt(EstadoPartida.PessoaVisao::moral).average().orElse(0));
        var s = e.seguranca();
        out.printf(" Acidentes: %d | dias perdidos: %d | HHT: %s | TF: %s | TG: %s | demissões: %d%n",
                s.acidentes(), s.diasPerdidos(), s.horasTrabalhadas().toPlainString(),
                s.taxaFrequencia().toPlainString(), s.taxaGravidade().toPlainString(), s.demissoes());

        out.println();
        out.println(" Fornecedores (scorecard do último fechamento)");
        out.println(" ------------------------------------------------------------");
        for (EstadoPartida.FornecedorVisao f : e.fornecedores()) {
            out.printf(" %-26s nota %s (%3d) | PPM %6d | entregas no prazo %d/%d | 8D no prazo %d/%d | chargeback R$ %s%n",
                    f.nome(), f.nota(), f.pontos(), f.ppm(), f.pedidosNoPrazo(), f.pedidosEntregues(),
                    f.oitoDsNoPrazo(), f.oitoDsRespondidos(), f.totalChargeback().toPlainString());
        }
        out.println(" Estoque (kits): " + new TreeMap<>(e.estoque()) + " | pedidos a caminho: " + e.pedidosACaminho().size());

        Map<TipoEvento, Long> porTipo = eventos.stream()
                .collect(Collectors.groupingBy(Evento::tipo, TreeMap::new, Collectors.counting()));
        out.println(" Eventos: " + porTipo);

        sim.conselho().ifPresent(r -> imprimirConselho(out, r));

        if (a.listarEventos()) {
            out.println();
            out.println(" Linha do tempo");
            out.println(" ------------------------------------------------------------");
            for (Evento ev : eventos) {
                out.printf(" %s %02dh  %s%n", DATA.format(ev.data()), ev.hora(), ev.mensagem());
            }
        }
    }

    static void imprimirConselho(PrintStream out, RelatorioConselho r) {
        out.println();
        out.println("==============================================================");
        out.printf(" REUNIÃO DO CONSELHO  |  mês %d (%s)%n", r.mes(), r.mesReferencia());
        out.println("==============================================================");
        out.println(" Metas");
        for (RelatorioConselho.Meta m : r.metas()) {
            out.printf("  [%s] %-38s %s%n", m.cumprida() ? "x" : " ", m.descricao(), m.resultado());
        }
        out.printf(" Paciência do conselho: %d -> %d (de 5)%n", r.pacienciaAntes(), r.pacienciaDepois());
        out.println();
        out.println(" DRE do mês (R$)");
        Dre d = r.dre();
        linha(out, "Receita reconhecida", d.receita());
        linha(out, "(-) Material", d.custoMaterial().negate());
        linha(out, "(-) Consumíveis", d.consumiveis().negate());
        linha(out, "(-) Material de retrabalho", d.materialRetrabalho().negate());
        linha(out, "(-) Pessoal (salários + encargos)", d.pessoal().negate());
        linha(out, "(-) Despesas fixas", d.despesasFixas().negate());
        linha(out, "(+) Chargeback recuperado", d.chargebackRecuperado());
        linha(out, "= EBITDA", d.ebitda());
        out.println();
        linha(out, "Caixa", r.caixa());
        linha(out, "Valor da empresa (EBITDA x 12 x 6)", r.valorEmpresa());
        if (r.descontoValuation().signum() > 0) {
            out.printf("   desconto de %s%% por acidentes e NCs escapadas%n",
                    r.descontoValuation().movePointRight(2).stripTrailingZeros().toPlainString());
        }
        out.printf(" Entregas: %d | NCs: %d | RNCs: %d | concessões: %d | acidentes: %d | TF %s | TG %s%n",
                r.barcosEntregues(), r.ncsDetectadas(), r.rncs(), r.concessoes(), r.acidentes(),
                r.taxaFrequencia().toPlainString(), r.taxaGravidade().toPlainString());
        if (r.alertaCaixaNegativo()) {
            out.println(" !!! ALERTA: caixa negativo");
        }
        out.println();
        for (RelatorioConselho.Fala f : r.falas()) {
            out.printf(" %s (%s): \"%s\"%n", f.conselheiro(), f.papel(), f.texto());
        }
        if (r.derrota() != null) {
            out.println();
            out.println(" FIM DE JOGO: " + r.derrota());
            return;
        }
        out.println();
        out.println(" Decisões para o mês seguinte");
        for (RelatorioConselho.Opcao o : r.opcoes()) {
            out.printf("  - %-32s R$ %12s  %s%n", o.titulo(), Dinheiro.formatar(o.custo()), o.descricao());
        }
    }

    private static void linha(PrintStream out, String rotulo, java.math.BigDecimal valor) {
        out.printf("  %-36s %16s%n", rotulo, Dinheiro.formatar(valor));
    }

    record Argumentos(long semente, int meses, Path dados, boolean listarEventos, boolean decidirNcs) {

        static Argumentos ler(String[] args) {
            long semente = 42;
            int meses = 1;
            Path dados = null;
            boolean eventos = false;
            boolean decidir = true;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--semente" -> semente = Long.parseLong(valor(args, ++i, "--semente"));
                    case "--meses" -> meses = Integer.parseInt(valor(args, ++i, "--meses"));
                    case "--dados" -> dados = Path.of(valor(args, ++i, "--dados"));
                    case "--eventos" -> eventos = true;
                    case "--sem-decisoes" -> decidir = false;
                    default -> throw new IllegalArgumentException("Argumento desconhecido: " + args[i]);
                }
            }
            if (meses < 1) {
                throw new IllegalArgumentException("--meses deve ser pelo menos 1");
            }
            return new Argumentos(semente, meses, dados, eventos, decidir);
        }

        private static String valor(String[] args, int i, String nome) {
            if (i >= args.length) {
                throw new IllegalArgumentException("Falta o valor de " + nome);
            }
            return args[i];
        }
    }
}
