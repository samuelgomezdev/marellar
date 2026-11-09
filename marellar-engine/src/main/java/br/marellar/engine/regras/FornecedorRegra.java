package br.marellar.engine.regras;

import br.marellar.engine.Partida;
import br.marellar.engine.Relogio;
import br.marellar.engine.dados.ParametrosFornecedores;
import br.marellar.engine.dados.ParametrosFornecedores.ParametrosPerfil;
import br.marellar.engine.dados.ParametrosLinha;
import br.marellar.engine.dominio.Barco;
import br.marellar.engine.dominio.Fornecedor;
import br.marellar.engine.dominio.Gravidade;
import br.marellar.engine.dominio.NaoConformidade;
import br.marellar.engine.dominio.Pedido;
import br.marellar.engine.dominio.Rnc;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.StatusRnc;
import br.marellar.engine.dominio.TipoEvento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Fornecedores e material (README 5.3 e 5.7):
 * <ul>
 *   <li>Cada gate com material consome 1 kit do estoque ao comecar; sem kit, o barco para.</li>
 *   <li>Politica de compra automatica: repoe ate o estoque minimo, contando o que esta a caminho.</li>
 *   <li>Atraso e PPM saem do perfil escondido do fornecedor.</li>
 *   <li>RNC: chargeback = custo documentado x (1 + taxa administrativa), descontado na proxima compra.
 *       8D vencido em NC critica gera chargeback das horas paradas.</li>
 *   <li>Scorecard mensal: entrega 40%, qualidade 30%, custo 20%, resposta 10%; nota A a F.</li>
 * </ul>
 */
public final class FornecedorRegra {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final BigDecimal MILHAO = BigDecimal.valueOf(1_000_000);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM");

    private FornecedorRegra() {
    }

    public static void fimDoDia(Partida partida) {
        receberEntregas(partida);
        acompanhar8d(partida);
        reporEstoque(partida);
    }

    // ----- material -----

    /** Consome o kit do gate atual. Devolve false (e para o barco) se nao houver kit. */
    public static boolean consumirMaterial(Partida partida, Barco barco) {
        String item = partida.parametros().linha33().itemDoGate(barco.gateAtual()).orElse(null);
        if (item == null) {
            return true;
        }
        if (partida.kitsEmEstoque(item) > 0) {
            partida.somarEstoque(item, -1);
            FinanceiroRegra.materialConsumido(partida, item);
            if (barco.status() == StatusBarco.PARADO_MATERIAL) {
                barco.mudarStatus(StatusBarco.EM_PRODUCAO);
            }
            return true;
        }
        if (barco.status() != StatusBarco.PARADO_MATERIAL) {
            barco.mudarStatus(StatusBarco.PARADO_MATERIAL);
            partida.registrarEvento(TipoEvento.PARADO_MATERIAL, barco.id(), "eventos.paradoMaterial",
                    Map.of("barco", barco.id(), "gate", barco.gateAtual().name(), "item", item));
        }
        return false;
    }

    private static void receberEntregas(Partida partida) {
        ParametrosLinha linha = partida.parametros().linha33();
        for (Pedido pedido : partida.pedidos()) {
            if (pedido.diaChegada() != partida.hoje()) {
                continue;
            }
            Fornecedor f = partida.fornecedor(pedido.fornecedorId()).orElseThrow();
            ParametrosPerfil perfil = partida.parametros().fornecedores().perfil(f.perfil());
            long pecas = (long) pedido.kits() * linha.pecasPorKit();
            BigDecimal probRejeicao = BigDecimal.valueOf(perfil.ppmBase()).divide(MILHAO, 6, RoundingMode.HALF_EVEN);
            long rejeitadas = 0;
            for (long i = 0; i < pecas; i++) {
                if (QualidadeRegra.sortear(partida, probRejeicao)) {
                    rejeitadas++;
                }
            }
            f.registrarRecebimento(pecas, rejeitadas, !pedido.atrasado());
            partida.somarEstoque(pedido.item(), pedido.kits());
            FinanceiroRegra.pagarCompra(partida, pedido, f.nome());
            long diasAtraso = pedido.diaChegada() - pedido.diaPrevisto();
            partida.registrarEvento(TipoEvento.MATERIAL, null, "eventos.materialChegou", Map.of(
                    "kits", String.valueOf(pedido.kits()), "item", pedido.item(), "fornecedor", f.nome(),
                    "atraso", diasAtraso > 0 ? " com " + diasAtraso + " dia(s) de atraso" : ""));
        }
    }

    private static void reporEstoque(Partida partida) {
        ParametrosLinha linha = partida.parametros().linha33();
        ParametrosFornecedores regras = partida.parametros().fornecedores();
        for (String item : linha.itens()) {
            int aCaminho = partida.pedidos().stream()
                    .filter(p -> p.item().equals(item) && p.diaChegada() > partida.hoje())
                    .mapToInt(Pedido::kits).sum();
            int disponivel = partida.kitsEmEstoque(item) + aCaminho;
            if (disponivel >= linha.estoqueMinimoKits()) {
                continue;
            }
            fazerPedido(partida, item, linha.estoqueMinimoKits() + 1 - disponivel);
        }
    }

    /** Pede kits ao fornecedor do item; o atraso e sorteado pelo perfil e o credito de chargeback abate o valor. */
    public static Pedido fazerPedido(Partida partida, String item, int kits) {
        ParametrosLinha linha = partida.parametros().linha33();
        ParametrosFornecedores regras = partida.parametros().fornecedores();
        Fornecedor f = partida.fornecedores().stream().filter(x -> x.itens().contains(item)).findFirst().orElseThrow();
        ParametrosPerfil perfil = regras.perfil(f.perfil());
        long previsto = partida.hoje() + perfil.diasEntrega();
        long chegada = previsto;
        if (QualidadeRegra.sortear(partida, perfil.probabilidadeAtraso())) {
            chegada += partida.aleatorio().proximoInt(regras.diasAtrasoMin(), regras.diasAtrasoMax() + 1);
        }
        BigDecimal bruto = linha.valorDoKit(item).multiply(perfil.fatorPreco()).multiply(BigDecimal.valueOf(kits))
                .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal desconto = f.usarCredito(bruto);
        Pedido pedido = new Pedido(partida.proximoIdPedido(), f.id(), item, kits, bruto, desconto,
                partida.hoje(), previsto, chegada);
        partida.pedidos().add(pedido);
        String data = DATA.format(Relogio.somarDiasUteis(partida.relogio().inicio(), previsto));
        partida.registrarEvento(TipoEvento.MATERIAL, null, "eventos.pedidoFeito", Map.of(
                "kits", String.valueOf(kits), "item", item, "fornecedor", f.nome(), "data", data));
        return pedido;
    }

    // ----- RNC, 8D e chargeback -----

    public static Rnc abrirRnc(Partida partida, NaoConformidade nc, Barco barco) {
        ParametrosFornecedores regras = partida.parametros().fornecedores();
        Fornecedor f = partida.fornecedor(nc.fornecedorId().orElseThrow()).orElseThrow();
        BigDecimal custo = custoDocumentado(nc.hhRetrabalho(), partida.parametros().linha33().custoHh(), nc.custoMaterial());
        BigDecimal chargeback = chargeback(custo, regras.taxaAdministrativaChargeback());
        long hoje = partida.hoje();
        Rnc rnc = new Rnc(partida.proximoIdRnc(), nc.id(), barco.id(), f.id(), nc.gravidade(), custo, chargeback,
                hoje, hoje + regras.prazo8dDiasUteis(), hoje + regras.perfil(f.perfil()).diasResposta8d());
        partida.rncs().add(rnc);
        f.lancarChargeback(chargeback);
        partida.mes().somarChargeback(chargeback);
        partida.mes().contarRnc();
        partida.registrarEvento(TipoEvento.RNC_ABERTA, barco.id(), "eventos.rncAberta", Map.of(
                "barco", barco.id(), "nc", nc.id(), "fornecedor", f.nome(), "valor", chargeback.toPlainString()));
        return rnc;
    }

    /** Custo documentado = HH x custo da HH + material. (Frete e horas de linha parada entram no 8D vencido.) */
    public static BigDecimal custoDocumentado(BigDecimal hh, BigDecimal custoHh, BigDecimal material) {
        return hh.multiply(custoHh).add(material).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Chargeback = custo documentado x (1 + taxa administrativa). */
    public static BigDecimal chargeback(BigDecimal custoDocumentado, BigDecimal taxa) {
        return custoDocumentado.multiply(BigDecimal.ONE.add(taxa)).setScale(2, RoundingMode.HALF_EVEN);
    }

    private static void acompanhar8d(Partida partida) {
        ParametrosFornecedores regras = partida.parametros().fornecedores();
        long hoje = partida.hoje();
        for (Rnc rnc : partida.rncs()) {
            if (!rnc.aguardandoResposta()) {
                continue;
            }
            Fornecedor f = partida.fornecedor(rnc.fornecedorId()).orElseThrow();
            if (rnc.status() == StatusRnc.AGUARDANDO_8D && hoje > rnc.prazo8d() && rnc.diaResposta8d() > rnc.prazo8d()) {
                BigDecimal extra = BigDecimal.ZERO;
                if (rnc.gravidade() == Gravidade.CRITICA) {
                    BigDecimal hhParadas = partida.nc(rnc.ncId()).map(NaoConformidade::hhRetrabalho).orElse(BigDecimal.ZERO);
                    extra = chargeback(hhParadas.multiply(partida.parametros().linha33().custoHh()),
                            regras.taxaAdministrativaChargeback());
                    f.lancarChargeback(extra);
                    partida.mes().somarChargeback(extra);
                }
                rnc.vencer(extra);
                partida.registrarEvento(TipoEvento.RNC_ABERTA, rnc.barcoId(), "eventos.oitoDVencido", Map.of(
                        "rnc", rnc.id(), "fornecedor", f.nome(),
                        "extra", extra.signum() > 0 ? " Chargeback das horas paradas: R$ " + extra.toPlainString() + "." : ""));
            }
            if (hoje >= rnc.diaResposta8d()) {
                boolean noPrazo = rnc.diaResposta8d() <= rnc.prazo8d();
                rnc.responder();
                f.registrarResposta8d(noPrazo);
                partida.registrarEvento(TipoEvento.RNC_ABERTA, rnc.barcoId(), "eventos.oitoDRespondido", Map.of(
                        "rnc", rnc.id(), "fornecedor", f.nome(), "atraso", noPrazo ? "" : ", com atraso"));
            }
        }
    }

    /** Multiplica a probabilidade de NC de origem fornecedor: ppmBase do perfil / ppmReferencia, com limites. */
    public static BigDecimal fatorFornecedor(Partida partida, String fornecedorId) {
        if (fornecedorId == null) {
            return BigDecimal.ONE;
        }
        ParametrosFornecedores regras = partida.parametros().fornecedores();
        Fornecedor f = partida.fornecedor(fornecedorId).orElseThrow();
        return fatorPorPpm(regras.perfil(f.perfil()).ppmBase(), regras);
    }

    public static BigDecimal fatorPorPpm(int ppmBase, ParametrosFornecedores regras) {
        BigDecimal fator = BigDecimal.valueOf(ppmBase)
                .divide(BigDecimal.valueOf(regras.ppmReferencia()), 4, RoundingMode.HALF_EVEN);
        return fator.max(regras.fatorFornecedorMin()).min(regras.fatorFornecedorMax());
    }

    // ----- scorecard mensal -----

    public static void scorecard(Partida partida) {
        ParametrosFornecedores regras = partida.parametros().fornecedores();
        for (Fornecedor f : partida.fornecedores()) {
            int pontos = pontosScorecard(f, regras, partida);
            String nota = regras.nota(pontos);
            f.definirNota(nota, pontos);
            partida.registrarEvento(TipoEvento.SCORECARD, null, "eventos.scorecard", Map.of(
                    "fornecedor", f.nome(), "nota", nota, "pontos", String.valueOf(pontos)));
        }
    }

    static int pontosScorecard(Fornecedor f, ParametrosFornecedores regras, Partida partida) {
        int entrega = f.pedidosEntregues() == 0 ? 100 : f.pedidosNoPrazo() * 100 / f.pedidosEntregues();
        int qualidade = limitar(100 - (int) (f.ppm() * 25 / regras.ppmReferencia()));
        int custo = limitar(100 - regras.perfil(f.perfil()).fatorPreco().subtract(new BigDecimal("0.85"))
                .multiply(BigDecimal.valueOf(200)).setScale(0, RoundingMode.HALF_EVEN).intValue());
        long vencidosSemResposta = partida.rncs().stream()
                .filter(r -> r.fornecedorId().equals(f.id()) && r.status() == StatusRnc.OITO_D_VENCIDO).count();
        long total8d = f.oitoDsRespondidos() + vencidosSemResposta;
        int resposta = total8d == 0 ? 100 : (int) (f.oitoDsNoPrazo() * 100 / total8d);
        var pesos = regras.pesosScorecard();
        int soma = entrega * pesos.entrega() + qualidade * pesos.qualidade() + custo * pesos.custo() + resposta * pesos.resposta();
        return BigDecimal.valueOf(soma).divide(CEM, 0, RoundingMode.HALF_EVEN).intValue();
    }

    private static int limitar(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
