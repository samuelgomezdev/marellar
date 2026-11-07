package br.marellar.engine.regras;

import br.marellar.engine.Partida;
import br.marellar.engine.agentes.Inspetor;
import br.marellar.engine.dados.CatalogoNc;
import br.marellar.engine.dados.ParametrosPessoas.Personalidade;
import br.marellar.engine.dominio.Barco;
import br.marellar.engine.dominio.DecisaoNc;
import br.marellar.engine.dominio.Gravidade;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.NaoConformidade;
import br.marellar.engine.dominio.Origem;
import br.marellar.engine.dominio.Pessoa;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.StatusNc;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.ordens.OrdemInvalidaException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Qualidade (README 5.4):
 * <ol>
 *   <li>Ao terminar um gate, sorteia NCs com {@code probabilidade x fatorNivelEquipe x fatorFornecedor}.</li>
 *   <li>Ao fim da inspecao, o inspetor detecta cada NC (ou ela escapa sem o jogador saber).</li>
 *   <li>O jogador decide cada NC detectada: retrabalho, concessao ou RNC contra o fornecedor.</li>
 *   <li>O gate so e aprovado quando nao ha NC pendente. NC critica nao aceita concessao.</li>
 * </ol>
 * Toda aleatoriedade sai do gerador da partida, na mesma ordem, para manter o determinismo.
 */
public final class QualidadeRegra {

    private QualidadeRegra() {
    }

    // ----- 1. sorteio ao terminar o gate -----

    public static List<NaoConformidade> sortearNcs(Partida partida, Barco barco) {
        CatalogoNc catalogo = partida.parametros().catalogoNc();
        BigDecimal fatorEquipe = catalogo.fatorDoNivelDaEquipe(barco.nivelMedioNoGate());
        List<NaoConformidade> novas = new ArrayList<>();
        for (CatalogoNc.TipoNc tipo : catalogo.tiposDoGate(barco.gateAtual())) {
            // fatorFornecedor = 1 ate o M4, quando o perfil do fornecedor entra no sorteio
            BigDecimal prob = tipo.probabilidade().multiply(fatorEquipe).min(BigDecimal.ONE);
            boolean ocorreu = sortear(partida, prob);
            if (ocorreu && novas.size() < catalogo.maxNcsPorGate()) {
                novas.add(new NaoConformidade(partida.proximoIdNc(), barco.id(), barco.gateAtual(),
                        tipo.codigo(), tipo.descricao(), tipo.gravidade(), tipo.origem(),
                        partida.fornecedorDoItem(tipo.item()).orElse(null),
                        tipo.hhRetrabalho(), tipo.custoMaterial()));
            }
        }
        partida.adicionarNcs(novas);
        return novas;
    }

    // ----- 2. resultado da inspecao -----

    public static void aposInspecao(Partida partida, Barco barco) {
        CatalogoNc catalogo = partida.parametros().catalogoNc();
        Pessoa inspetor = partida.inspetor();
        Personalidade personalidade = inspetor.personalidade().orElseThrow();
        int fila = partida.filaInspecao().size();

        List<NaoConformidade> detectadas = new ArrayList<>();
        for (NaoConformidade nc : partida.ncsDoGateAtual(barco)) {
            if (nc.status() != StatusNc.PENDENTE_INSPECAO) {
                continue;
            }
            BigDecimal prob = Inspetor.probabilidadeDeteccao(nc.gravidade(), personalidade, fila, catalogo);
            if (sortear(partida, prob)) {
                nc.detectar();
                detectadas.add(nc);
            } else {
                nc.escapar();
            }
        }

        if (detectadas.isEmpty()) {
            InspecaoRegra.aprovarGate(partida, barco);
            return;
        }
        barco.mudarStatus(StatusBarco.AGUARDANDO_DECISAO);
        partida.registrarEvento(TipoEvento.NC_DETECTADA, barco.id(), "eventos.ncDetectada", Map.of(
                "barco", barco.id(), "gate", barco.gateAtual().name(),
                "quantidade", String.valueOf(detectadas.size()),
                "inspetor", primeiroNome(inspetor),
                "fala", fala(partida, "inspetor.ncDetectada")));
        detectadas.stream().filter(nc -> nc.gravidade() == Gravidade.CRITICA).findFirst().ifPresent(critica -> {
            partida.registrarEvento(TipoEvento.NC_CRITICA, barco.id(), "eventos.ncCritica", Map.of(
                    "barco", barco.id(), "gate", barco.gateAtual().name(), "descricao", critica.descricao()));
            partida.pausar(MotivoPausa.NC_CRITICA);
        });
    }

    // ----- 3. decisao do jogador -----

    public static void decidir(Partida partida, String ncId, DecisaoNc decisao) {
        NaoConformidade nc = partida.nc(ncId)
                .filter(NaoConformidade::visivel)
                .orElseThrow(() -> new OrdemInvalidaException("NC nao encontrada: " + ncId));
        if (nc.status() != StatusNc.ABERTA) {
            throw new OrdemInvalidaException("NC " + ncId + " ja foi decidida (" + nc.status() + ")");
        }
        if (decisao == DecisaoNc.CONCESSAO && nc.gravidade() == Gravidade.CRITICA) {
            throw new OrdemInvalidaException("NC critica nao aceita concessao: " + ncId);
        }
        if (decisao == DecisaoNc.RNC_FORNECEDOR && nc.origem() != Origem.FORNECEDOR) {
            throw new OrdemInvalidaException("RNC contra fornecedor so vale para NC de material comprado: " + ncId);
        }

        Barco barco = partida.barco(nc.barcoId()).orElseThrow();
        Pessoa inspetor = partida.inspetor();
        nc.decidir(decisao);
        switch (decisao) {
            case CONCESSAO -> {
                partida.registrarConcessao();
                CatalogoNc.Inspecao regras = partida.parametros().catalogoNc().inspecao();
                int queda = Inspetor.quedaMoralPorConcessao(inspetor.personalidade().orElseThrow(), regras);
                inspetor.definirMoral(inspetor.moral() - queda);
                partida.registrarEvento(TipoEvento.CONCESSAO, barco.id(), "eventos.concessao", Map.of(
                        "barco", barco.id(), "nc", nc.id(), "inspetor", primeiroNome(inspetor),
                        "fala", fala(partida, "inspetor.concessaoDemais")));
            }
            case RNC_FORNECEDOR -> {
                partida.registrarRnc();
                partida.somarCustoMaterialRetrabalho(nc.custoMaterial());
                String fornecedor = nc.fornecedorId().flatMap(partida::nomeDoFornecedor).orElse("fornecedor");
                partida.registrarEvento(TipoEvento.RNC_ABERTA, barco.id(), "eventos.rncAberta",
                        Map.of("barco", barco.id(), "nc", nc.id(), "fornecedor", fornecedor));
            }
            case RETRABALHO -> partida.somarCustoMaterialRetrabalho(nc.custoMaterial());
        }
        seguirDepoisDasDecisoes(partida, barco);
    }

    /** Com todas as NCs do gate decididas: comeca o retrabalho ou aprova o gate. */
    static void seguirDepoisDasDecisoes(Partida partida, Barco barco) {
        List<NaoConformidade> doGate = partida.ncsDoGateAtual(barco);
        if (doGate.stream().anyMatch(nc -> nc.status() == StatusNc.ABERTA)) {
            return;
        }
        BigDecimal hh = doGate.stream()
                .filter(nc -> nc.status() == StatusNc.EM_RETRABALHO)
                .map(NaoConformidade::hhRetrabalho)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (hh.signum() > 0) {
            barco.iniciarRetrabalho(hh);
        } else {
            InspecaoRegra.aprovarGate(partida, barco);
        }
    }

    public static void retrabalhoConcluido(Partida partida, Barco barco) {
        partida.ncsDoGateAtual(barco).stream()
                .filter(nc -> nc.status() == StatusNc.EM_RETRABALHO)
                .forEach(NaoConformidade::resolver);
        partida.registrarEvento(TipoEvento.RETRABALHO_CONCLUIDO, barco.id(), "eventos.retrabalhoConcluido",
                Map.of("barco", barco.id(), "gate", barco.gateAtual().name()));
        InspecaoRegra.aprovarGate(partida, barco);
    }

    // ----- utilitarios -----

    /** Sorteio de Bernoulli com o gerador da partida. Probabilidade 0 nunca acontece; 1 sempre. */
    static boolean sortear(Partida partida, BigDecimal probabilidade) {
        double sorteio = partida.aleatorio().nextDouble();
        return BigDecimal.valueOf(sorteio).compareTo(probabilidade) < 0;
    }

    private static String fala(Partida partida, String caminho) {
        int quantidade = partida.parametros().textos().quantidadeFalas(caminho);
        return partida.parametros().textos().fala(caminho, partida.aleatorio().nextInt(Math.max(1, quantidade)));
    }

    private static String primeiroNome(Pessoa p) {
        return p.nome().split(" ")[0];
    }
}
