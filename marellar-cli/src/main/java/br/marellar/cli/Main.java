package br.marellar.cli;

import br.marellar.engine.Relogio;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.DadosInvalidosException;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dominio.Evento;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.estado.EstadoPartida;
import br.marellar.engine.estado.EstadoPartida.BarcoVisao;

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
 * Roda uma partida inteira pelo terminal, sem ordens do jogador, e imprime o resumo.
 * <pre>
 * java -jar marellar-cli.jar --semente 42 --meses 1 [--dados caminho/para/data] [--eventos]
 * </pre>
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
            System.err.println("Uso: java -jar marellar-cli.jar --semente 42 --meses 1 [--dados pasta] [--eventos]");
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
            rodados += sim.avancar((int) Math.min(Integer.MAX_VALUE, alvo - rodados)).ticksExecutados();
        }

        imprimir(out, sim, a);
    }

    static void imprimir(PrintStream out, Simulacao sim, Argumentos a) {
        EstadoPartida e = sim.estado();
        List<Evento> eventos = sim.eventosDesde(0);

        out.println("==============================================================");
        out.printf(" MARELLAR  |  semente %d  |  %d mes(es)%n", a.semente(), a.meses());
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

        Map<TipoEvento, Long> porTipo = eventos.stream()
                .collect(Collectors.groupingBy(Evento::tipo, TreeMap::new, Collectors.counting()));
        out.println(" Eventos: " + porTipo);

        if (a.listarEventos()) {
            out.println();
            out.println(" Linha do tempo");
            out.println(" ------------------------------------------------------------");
            for (Evento ev : eventos) {
                out.printf(" %s %02dh  %s%n", DATA.format(ev.data()), ev.hora(), ev.mensagem());
            }
        }
    }

    record Argumentos(long semente, int meses, Path dados, boolean listarEventos) {

        static Argumentos ler(String[] args) {
            long semente = 42;
            int meses = 1;
            Path dados = null;
            boolean eventos = false;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--semente" -> semente = Long.parseLong(valor(args, ++i, "--semente"));
                    case "--meses" -> meses = Integer.parseInt(valor(args, ++i, "--meses"));
                    case "--dados" -> dados = Path.of(valor(args, ++i, "--dados"));
                    case "--eventos" -> eventos = true;
                    default -> throw new IllegalArgumentException("Argumento desconhecido: " + args[i]);
                }
            }
            if (meses < 1) {
                throw new IllegalArgumentException("--meses deve ser pelo menos 1");
            }
            return new Argumentos(semente, meses, dados, eventos);
        }

        private static String valor(String[] args, int i, String nome) {
            if (i >= args.length) {
                throw new IllegalArgumentException("Falta o valor de " + nome);
            }
            return args[i];
        }
    }
}
