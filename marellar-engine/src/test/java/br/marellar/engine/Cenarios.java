package br.marellar.engine;

import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.CatalogoNc;
import br.marellar.engine.dados.ParametrosEventos;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dados.ParametrosLinha.BarcoInicial;
import br.marellar.engine.dados.ParametrosPessoas;
import br.marellar.engine.dominio.CodigoGate;

import java.math.BigDecimal;
import java.util.List;

/** Parametros prontos para os testes: tira o acaso que nao interessa ao teste. */
public final class Cenarios {

    public static final ParametrosJogo PADRAO = CarregadorDados.carregarDoClasspath();

    private Cenarios() {
    }

    /** Sem NC, sem acidente, sem demissao e sem quebra de peca. */
    public static ParametrosJogo semAcaso(ParametrosJogo p) {
        return semEventos(semNcs(p));
    }

    public static ParametrosJogo semNcs(ParametrosJogo p) {
        CatalogoNc c = p.catalogoNc();
        return p.comCatalogoNc(c.comTipos(c.tipos().stream().map(t -> t.comProbabilidade(BigDecimal.ZERO)).toList()));
    }

    public static ParametrosJogo semEventos(ParametrosJogo p) {
        return comEventos(p, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public static ParametrosJogo comEventos(ParametrosJogo p, BigDecimal quebra, BigDecimal acidente,
                                            BigDecimal demissaoBase, BigDecimal demissaoMoralBaixa) {
        ParametrosEventos e = p.eventos();
        var q = e.quebraPeca();
        var a = e.acidente();
        var d = e.demissao();
        return p.comEventos(new ParametrosEventos(
                new ParametrosEventos.QuebraPeca(quebra, q.multiplicadorPressa(), q.multiplicadorMoralBaixa(),
                        q.hhRetrabalho(), q.custoMaterial()),
                new ParametrosEventos.Acidente(acidente, a.multiplicadorTreinoVencido(), a.multiplicadorSobrecarga(),
                        a.diasPerdidosMin(), a.diasPerdidosMax(), a.quedaMoralEquipe()),
                new ParametrosEventos.Demissao(demissaoBase, demissaoMoralBaixa, d.limiarMoralBaixa())));
    }

    public static ParametrosJogo comBarcos(ParametrosJogo p, BarcoInicial... barcos) {
        return p.comLinha33(p.linha33().comBarcosIniciais(List.of(barcos)));
    }

    public static ParametrosJogo umBarcoNoRe(ParametrosJogo p) {
        return comBarcos(p, new BarcoInicial("33-100", "Teste", CodigoGate.RE));
    }

    /** Toda a equipe de producao com a mesma moral. */
    public static ParametrosJogo comMoralDaEquipe(ParametrosJogo p, int moral) {
        List<ParametrosPessoas.PessoaInicial> equipe = p.pessoas().equipe().stream()
                .map(x -> new ParametrosPessoas.PessoaInicial(x.id(), x.nome(), x.funcao(), x.nivel(),
                        moral, x.personalidade(), x.treinoSegurancaEmDia()))
                .toList();
        return p.comPessoas(p.pessoas().comEquipe(equipe));
    }

    /** Avanca dias uteis inteiros, mesmo que o motor pause no meio (fim do mes, acidente...). */
    public static void avancarDias(Simulacao sim, int dias) {
        long alvo = sim.estado().tick() + (long) dias * Relogio.TICKS_POR_DIA;
        while (sim.estado().tick() < alvo) {
            sim.avancar((int) Math.min(Relogio.TICKS_POR_MES, alvo - sim.estado().tick()));
        }
    }
}
