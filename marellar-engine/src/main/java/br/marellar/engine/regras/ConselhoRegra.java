package br.marellar.engine.regras;

import br.marellar.engine.MesContabil;
import br.marellar.engine.Partida;
import br.marellar.engine.dados.ParametrosFinanceiro;
import br.marellar.engine.dados.Textos;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.MotivoDerrota;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.Pessoa;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.dominio.TipoLancamento;
import br.marellar.engine.ordens.OrdemInvalidaException;
import br.marellar.engine.relatorio.Dinheiro;
import br.marellar.engine.relatorio.Dre;
import br.marellar.engine.relatorio.RelatorioConselho;
import br.marellar.engine.relatorio.RelatorioConselho.Fala;
import br.marellar.engine.relatorio.RelatorioConselho.Meta;
import br.marellar.engine.relatorio.RelatorioConselho.Opcao;
import br.marellar.engine.relatorio.Valuation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reuniao do conselho no fim do mes (README 5.10): metas, falas, paciencia de 0 a 5 (so termometro),
 * valor da empresa, alerta de caixa negativo e 4 decisoes para o mes seguinte.
 * Derrota: caixa negativo por 3 meses seguidos ou meses ruins seguidos com o conselho
 * (mes ruim = metasPerdidasParaMesRuim ou mais metas perdidas; DEC-0058 e DEC-0062 da wiki).
 */
public final class ConselhoRegra {

    public static final List<String> OPCOES = List.of("TREINAR_NR", "CONTRATAR_2", "ANTECIPAR_COMPRA", "ENCONTRO_EQUIPE");

    private ConselhoRegra() {
    }

    public static RelatorioConselho reunir(Partida partida, Dre dre) {
        ParametrosFinanceiro regras = partida.parametros().financeiro();
        ParametrosFinanceiro.Conselho metasConselho = regras.conselho();
        Textos textos = partida.parametros().textos();
        MesContabil mes = partida.mes();

        // ----- metas
        List<Meta> metas = metas(partida);

        // ----- paciencia
        int antes = partida.pacienciaConselho();
        int depois = antes;
        for (Meta m : metas) {
            depois += m.cumprida() ? 1 : -1;
        }
        depois = Math.max(0, Math.min(metasConselho.pacienciaMaxima(), depois));
        partida.definirPacienciaConselho(depois);

        // ----- mes ruim (paciencia acima e so termometro)
        long perdidas = metas.stream().filter(m -> !m.cumprida()).count();
        boolean mesRuim = perdidas >= metasConselho.metasPerdidasParaMesRuim();
        partida.definirMesesRuinsSeguidos(mesRuim ? partida.mesesRuinsSeguidos() + 1 : 0);

        // ----- caixa negativo
        boolean negativo = partida.caixa().signum() < 0;
        partida.definirMesesCaixaNegativo(negativo ? partida.mesesCaixaNegativo() + 1 : 0);

        // ----- valor da empresa
        Valuation v = Valuation.calcular(dre.ebitda(), mes.acidentes(), mes.ncsEscapadas(), regras);
        partida.definirValorEmpresa(v.valor());

        // ----- derrota
        MotivoDerrota derrota = null;
        if (partida.mesesCaixaNegativo() >= regras.mesesCaixaNegativoParaDerrota()) {
            derrota = MotivoDerrota.CAIXA;
        } else if (partida.mesesRuinsSeguidos() >= metasConselho.mesesRuinsParaDemissao()) {
            derrota = MotivoDerrota.CONSELHO;
        }

        var r = partida.relogio();
        String mesRef = r.rotuloMesReferencia();
        var seguranca = partida;
        RelatorioConselho relatorio = new RelatorioConselho(
                r.mes() + 1, mesRef, metas, dre, partida.caixa(), v.valor(), v.descontoPercentual(),
                mes.barcosEntregues(), mes.ncsDetectadas(), mes.rncs(), mes.concessoes(), mes.acidentes(),
                EventoRegra.taxaFrequencia(seguranca.acidentes(), seguranca.horasTrabalhadas()),
                EventoRegra.taxaGravidade(seguranca.diasPerdidos(), seguranca.horasTrabalhadas()),
                antes, depois, mesRuim, partida.mesesRuinsSeguidos(), negativo, falas(partida, metas, negativo, dre), derrota == null ? opcoes(partida) : List.of(),
                null, derrota);
        partida.conselhos().add(relatorio);

        if (derrota != null) {
            partida.perder(derrota);
            partida.registrarEvento(TipoEvento.DERROTA, null, "eventos.derrota",
                    Map.of("motivo", textos.formatar("conselho.derrota." + derrota.name(), Map.of())));
        }
        partida.novoMesContabil();
        return relatorio;
    }

    /**
     * As 4 metas do mes com o que aconteceu ate agora. Na reuniao (ultima hora do mes) e o resultado do mes;
     * no meio do mes, e o painel de gestao. "Cumprida" no meio do mes = se o mes acabasse agora.
     */
    public static List<Meta> metas(Partida partida) {
        ParametrosFinanceiro.Conselho c = partida.parametros().financeiro().conselho();
        Textos textos = partida.parametros().textos();
        MesContabil mes = partida.mes();
        int barcosNoMes = (int) partida.barcos().stream().filter(b -> b.ativo()).count() + mes.barcosEntregues();
        BigDecimal ncPorBarco = BigDecimal.valueOf(mes.ncsDetectadas())
                .divide(BigDecimal.valueOf(Math.max(1, barcosNoMes)), 2, java.math.RoundingMode.HALF_EVEN);
        BigDecimal maxNc = BigDecimal.valueOf(c.metaNcPorBarcoMaxima());
        return List.of(
                meta(textos, 0, String.valueOf(c.metaCascosDm()), mes.gatesDmAprovados() >= c.metaCascosDm(),
                        mes.gatesDmAprovados() + " casco(s)", BigDecimal.valueOf(mes.gatesDmAprovados()),
                        BigDecimal.valueOf(c.metaCascosDm()), false),
                meta(textos, 1, Dinheiro.formatar(c.metaCaixaMinimo()), partida.caixa().compareTo(c.metaCaixaMinimo()) > 0,
                        "R$ " + Dinheiro.formatar(partida.caixa()), partida.caixa(), c.metaCaixaMinimo(), false),
                meta(textos, 2, "", mes.acidentes() == 0, mes.acidentes() + " acidente(s)",
                        BigDecimal.valueOf(mes.acidentes()), BigDecimal.ZERO, true),
                meta(textos, 3, String.valueOf(c.metaNcPorBarcoMaxima()), ncPorBarco.compareTo(maxNc) < 0,
                        ncPorBarco.toPlainString() + " NC/barco", ncPorBarco, maxNc, true));
    }

    private static Meta meta(Textos textos, int indice, String alvo, boolean cumprida, String resultado,
                             BigDecimal atual, BigDecimal valorAlvo, boolean limite) {
        return new Meta(textos.campo("metas", indice, "id"),
                textos.campo("metas", indice, "descricao").replace("{alvo}", alvo), cumprida, resultado,
                atual, valorAlvo, limite);
    }

    /** Conselheiro financeiro fala de caixa; a de operacoes, de seguranca e metas. Serio quando ha problema. */
    private static List<Fala> falas(Partida partida, List<Meta> metas, boolean caixaNegativo, Dre dre) {
        Textos textos = partida.parametros().textos();
        MesContabil mes = partida.mes();
        boolean caixaOk = metas.get(1).cumprida();
        boolean todasOk = metas.stream().allMatch(Meta::cumprida);
        BigDecimal limiteCompras = mes.custoMaterial().multiply(partida.parametros().financeiro().conselho().fatorMaterialDemais());
        boolean comprouDemais = mes.custoMaterial().signum() > 0 && mes.compras().compareTo(limiteCompras) > 0;

        String financeiro;
        if (caixaNegativo) {
            financeiro = "serio.caixaNegativo";
        } else if (!caixaOk) {
            financeiro = "serio.caixaRuim";
        } else if (dre.ebitda().signum() < 0) {
            financeiro = "serio.ebitdaNegativo";
        } else if (comprouDemais) {
            financeiro = "humor.materialDemais";
        } else if (todasOk) {
            financeiro = "serio.metaCumprida";
        } else {
            financeiro = "humor.tudoBem";
        }
        String operacoes;
        if (mes.acidentes() > 0) {
            operacoes = "serio.acidente";
        } else if (!todasOk) {
            operacoes = "serio.metaPerdida";
        } else if (mes.ncsDetectadas() == 0) {
            operacoes = "humor.zeroNc";
        } else {
            operacoes = "humor.tudoBem";
        }
        return List.of(fala(textos, 0, financeiro), fala(textos, 1, operacoes));
    }

    private static Fala fala(Textos textos, int conselheiro, String caminho) {
        return new Fala(textos.campo("conselheiros", conselheiro, "nome"), textos.campo("conselheiros", conselheiro, "papel"),
                textos.formatar("conselho." + caminho, Map.of()), caminho.startsWith("serio"));
    }

    private static List<Opcao> opcoes(Partida partida) {
        Textos textos = partida.parametros().textos();
        List<Opcao> lista = new ArrayList<>();
        for (String id : OPCOES) {
            lista.add(new Opcao(id, textos.formatar("conselho.opcoes." + id + ".titulo", Map.of()),
                    textos.formatar("conselho.opcoes." + id + ".descricao", Map.of()),
                    partida.parametros().financeiro().decisoes().getOrDefault(id, BigDecimal.ZERO)));
        }
        return lista;
    }

    // ----- decisao do jogador -----

    public static void escolher(Partida partida, String opcaoId) {
        RelatorioConselho r = partida.ultimoConselho()
                .orElseThrow(() -> new OrdemInvalidaException("Ainda nao houve reuniao do conselho"));
        if (r.decisaoEscolhida() != null) {
            throw new OrdemInvalidaException("A decisao deste mes ja foi tomada: " + r.decisaoEscolhida());
        }
        Opcao opcao = r.opcoes().stream().filter(o -> o.id().equals(opcaoId)).findFirst()
                .orElseThrow(() -> new OrdemInvalidaException("Opcao do conselho desconhecida: " + opcaoId));
        ParametrosFinanceiro regras = partida.parametros().financeiro();

        switch (opcaoId) {
            case "TREINAR_NR" -> producao(partida).forEach(p -> p.definirTreinoSegurancaEmDia(true));
            case "CONTRATAR_2" -> {
                PessoasRegra.contratar(partida, Funcao.PRODUCAO);
                PessoasRegra.contratar(partida, Funcao.PRODUCAO);
            }
            case "ANTECIPAR_COMPRA" -> partida.parametros().linha33().itens()
                    .forEach(item -> FornecedorRegra.fazerPedido(partida, item, regras.kitsAntecipados()));
            case "ENCONTRO_EQUIPE" -> producao(partida).forEach(p -> p.definirMoral(p.moral() + regras.ganhoMoralEncontro()));
            default -> throw new OrdemInvalidaException("Opcao sem efeito: " + opcaoId);
        }
        if (opcao.custo().signum() > 0) {
            partida.lancar(TipoLancamento.DECISAO_CONSELHO, opcao.custo().negate(), opcao.titulo());
        }
        partida.substituirUltimoConselho(r.comDecisao(opcaoId));
        partida.registrarEvento(TipoEvento.FINANCEIRO, null, "eventos.decisaoConselho", Map.of("titulo", opcao.titulo()));
    }

    private static List<Pessoa> producao(Partida partida) {
        return partida.pessoas().stream().filter(p -> p.naEmpresa() && p.funcao() == Funcao.PRODUCAO).toList();
    }

    /** Pausa de derrota: o motor para de avancar. */
    public static void pausarSeDerrota(Partida partida) {
        if (partida.derrota().isPresent()) {
            partida.pausar(MotivoPausa.DERROTA);
        }
    }
}
