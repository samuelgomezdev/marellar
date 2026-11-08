package br.marellar.engine.dados;

import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.Gravidade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Regras de consistencia dos parametros. Junta todos os erros e lanca uma unica
 * {@link DadosInvalidosException}, para quem edita o JSON corrigir tudo de uma vez.
 */
public final class ValidadorDados {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final List<String> erros = new ArrayList<>();

    private ValidadorDados() {
    }

    public static void validar(ParametrosJogo p) {
        ValidadorDados v = new ValidadorDados();
        v.linha(p.linha33());
        v.catalogo(p.catalogoNc());
        v.pessoas(p.pessoas());
        v.fornecedores(p.fornecedores());
        v.eventos(p.eventos());
        v.financeiro(p.financeiro());
        v.textos(p.textos());
        v.itensDeFornecedor(p.catalogoNc(), p.fornecedores());
        v.itensDaLinha(p.linha33(), p.fornecedores());
        if (!v.erros.isEmpty()) {
            throw new DadosInvalidosException(v.erros);
        }
    }

    private void linha(ParametrosLinha l) {
        String arq = CarregadorDados.LINHA_33;
        positivo(arq, "hhPorBarco", l.hhPorBarco());
        positivo(arq, "custoHh", l.custoHh());
        positivo(arq, "materialPorBarco", l.materialPorBarco());
        positivo(arq, "precoVenda", l.precoVenda());
        percentual(arq, "percentualConsumiveis", l.percentualConsumiveis());
        percentual(arq, "percentualImportados", l.percentualImportados());
        percentual(arq, "cotaContratoPercentual", l.cotaContratoPercentual());
        regra(l.prazoRecebimentoDias() >= 0, arq, "prazoRecebimentoDias nao pode ser negativo");
        regra(l.maxPessoasPorBarco() >= 1, arq, "maxPessoasPorBarco deve ser pelo menos 1");
        regra(l.ticksInspecao() >= 1, arq, "ticksInspecao deve ser pelo menos 1");
        regra(l.estoqueInicialKits() >= 0 && l.estoqueMinimoKits() >= 0, arq, "estoque nao pode ser negativo");
        regra(l.pecasPorKit() >= 1, arq, "pecasPorKit deve ser pelo menos 1");
        BigDecimal somaMaterial = soma(l.gates(), ParametrosLinha.Gate::materialPercentual);
        regra(somaMaterial.compareTo(CEM) == 0, arq,
                "a soma de materialPercentual dos gates deve ser 100; e " + somaMaterial.toPlainString());

        List<CodigoGate> codigos = l.gates().stream().map(ParametrosLinha.Gate::codigo).toList();
        regra(codigos.equals(Arrays.asList(CodigoGate.values())), arq,
                "gates devem ser exatamente " + Arrays.toString(CodigoGate.values()) + ", nesta ordem; veio " + codigos);

        BigDecimal somaPesos = soma(l.gates(), ParametrosLinha.Gate::pesoHhPercentual);
        regra(somaPesos.compareTo(CEM) == 0, arq, "a soma de pesoHhPercentual dos gates deve ser 100; e " + somaPesos.toPlainString());

        BigDecimal somaCotas = soma(l.gates(), ParametrosLinha.Gate::cotaPercentual)
                .add(nulo(l.cotaContratoPercentual()));
        regra(somaCotas.compareTo(CEM) == 0, arq,
                "cotaContratoPercentual + soma de cotaPercentual dos gates deve ser 100; e " + somaCotas.toPlainString());
        for (ParametrosLinha.Gate g : l.gates()) {
            percentual(arq, "gates[" + g.codigo() + "].pesoHhPercentual", g.pesoHhPercentual());
            percentual(arq, "gates[" + g.codigo() + "].cotaPercentual", g.cotaPercentual());
            regra(g.nome() != null && !g.nome().isBlank(), arq, "gates[" + g.codigo() + "].nome vazio");
        }

        Set<String> ids = new HashSet<>();
        for (ParametrosLinha.BarcoInicial b : l.barcosIniciais()) {
            regra(b.id() != null && !b.id().isBlank(), arq, "barcosIniciais: id vazio");
            regra(ids.add(b.id()), arq, "barcosIniciais: id repetido " + b.id());
            regra(b.gateInicial() != null, arq, "barcosIniciais[" + b.id() + "]: gateInicial ausente");
        }
    }

    private void catalogo(CatalogoNc c) {
        String arq = CarregadorDados.NC_CATALOGO;
        for (Gravidade g : Gravidade.values()) {
            BigDecimal d = c.deteccaoBase().get(g);
            regra(d != null, arq, "deteccaoBase sem valor para " + g);
            probabilidade(arq, "deteccaoBase." + g, d);
        }
        regra(c.maxNcsPorGate() >= 0, arq, "maxNcsPorGate nao pode ser negativo");
        IntStream.rangeClosed(1, 5).forEach(n -> positivo(arq, "fatorNivelEquipe." + n, c.fatorNivelEquipe().get(n)));
        CatalogoNc.Inspecao i = c.inspecao();
        regra(i != null, arq, "inspecao e obrigatorio");
        if (i != null) {
            positivo(arq, "inspecao.deteccaoBase", i.deteccaoBase());
            naoNegativo(arq, "inspecao.pesoRigorNaDeteccao", i.pesoRigorNaDeteccao());
            naoNegativo(arq, "inspecao.pesoVelocidadeNaDeteccao", i.pesoVelocidadeNaDeteccao());
            probabilidade(arq, "inspecao.pesoRigorNoTempo", i.pesoRigorNoTempo());
            probabilidade(arq, "inspecao.pesoVelocidadeNoTempo", i.pesoVelocidadeNoTempo());
            regra(i.filaSemPerda() >= 0, arq, "inspecao.filaSemPerda nao pode ser negativo");
            probabilidade(arq, "inspecao.perdaPorBarcoNaFila", i.perdaPorBarcoNaFila());
            probabilidade(arq, "inspecao.fatorCargaMinimo", i.fatorCargaMinimo());
            naoNegativo(arq, "inspecao.quedaMoralPorConcessao", i.quedaMoralPorConcessao());
        }
        regra(!c.tipos().isEmpty(), arq, "tipos nao pode ser vazio");
        Set<String> codigos = new HashSet<>();
        for (CatalogoNc.TipoNc t : c.tipos()) {
            String nome = "tipos[" + t.codigo() + "]";
            regra(t.codigo() != null && !t.codigo().isBlank(), arq, "tipo com codigo vazio");
            regra(codigos.add(t.codigo()), arq, "codigo repetido " + t.codigo());
            regra(t.gravidade() != null, arq, nome + ".gravidade ausente");
            regra(t.origem() != null, arq, nome + ".origem ausente");
            regra(!t.gates().isEmpty(), arq, nome + ".gates nao pode ser vazio");
            probabilidade(arq, nome + ".probabilidade", t.probabilidade());
            naoNegativo(arq, nome + ".hhRetrabalho", t.hhRetrabalho());
            naoNegativo(arq, nome + ".custoMaterial", t.custoMaterial());
        }
    }

    /** Toda NC de origem FORNECEDOR precisa de um item que algum fornecedor entrega. */
    private void itensDeFornecedor(CatalogoNc c, ParametrosFornecedores f) {
        for (CatalogoNc.TipoNc t : c.tipos()) {
            if (t.origem() != br.marellar.engine.dominio.Origem.FORNECEDOR) {
                continue;
            }
            boolean temFornecedor = t.item() != null
                    && f.fornecedores().stream().anyMatch(forn -> forn.itens().contains(t.item()));
            regra(temFornecedor, CarregadorDados.NC_CATALOGO,
                    "tipos[" + t.codigo() + "]: origem FORNECEDOR precisa de um item entregue por algum fornecedor ("
                            + t.item() + ")");
        }
    }

    /** Todo item consumido por um gate precisa de fornecedor; gate com item precisa de % de material. */
    private void itensDaLinha(ParametrosLinha l, ParametrosFornecedores f) {
        for (ParametrosLinha.Gate g : l.gates()) {
            if (g.item() == null) {
                continue;
            }
            regra(f.fornecedores().stream().anyMatch(forn -> forn.itens().contains(g.item())),
                    CarregadorDados.LINHA_33, "gates[" + g.codigo() + "].item sem fornecedor: " + g.item());
            regra(g.materialPercentual() != null && g.materialPercentual().signum() > 0, CarregadorDados.LINHA_33,
                    "gates[" + g.codigo() + "] tem item mas materialPercentual zero");
        }
    }

    private void pessoas(ParametrosPessoas p) {
        String arq = CarregadorDados.PESSOAS;
        IntStream.rangeClosed(1, 5).forEach(n -> {
            regra(p.fatorNivel().containsKey(n), arq, "fatorNivel sem o nivel " + n);
            regra(p.salarioPorNivel().containsKey(n), arq, "salarioPorNivel sem o nivel " + n);
            positivo(arq, "fatorNivel." + n, p.fatorNivel().get(n));
            positivo(arq, "salarioPorNivel." + n, p.salarioPorNivel().get(n));
        });
        positivo(arq, "salarioInspetor", p.salarioInspetor());
        regra(p.nivelMaximoPorTempo() >= 1 && p.nivelMaximoPorTempo() <= 5, arq, "nivelMaximoPorTempo deve estar entre 1 e 5");
        regra(p.diasParaSubirNivel() >= 1, arq, "diasParaSubirNivel deve ser pelo menos 1");
        regra(!p.nomesParaContratar().isEmpty(), arq, "nomesParaContratar nao pode ser vazio");
        regra(p.moralInicialContratado() >= 0 && p.moralInicialContratado() <= 100, arq,
                "moralInicialContratado deve estar entre 0 e 100");
        regra(p.diasEntreUmAUm() >= 0, arq, "diasEntreUmAUm nao pode ser negativo");

        Set<String> ids = new HashSet<>();
        int produtivos = 0;
        int inspetores = 0;
        for (ParametrosPessoas.PessoaInicial pessoa : p.equipe()) {
            String nome = "equipe[" + pessoa.id() + "]";
            regra(pessoa.id() != null && !pessoa.id().isBlank(), arq, "pessoa com id vazio");
            regra(ids.add(pessoa.id()), arq, "id repetido " + pessoa.id());
            regra(pessoa.funcao() != null, arq, nome + ".funcao ausente");
            regra(pessoa.nivel() >= 1 && pessoa.nivel() <= 5, arq, nome + ".nivel deve estar entre 1 e 5");
            regra(pessoa.moral() >= 0 && pessoa.moral() <= 100, arq, nome + ".moral deve estar entre 0 e 100");
            if (pessoa.funcao() == Funcao.PRODUCAO) {
                produtivos++;
            }
            if (pessoa.funcao() == Funcao.INSPETOR) {
                inspetores++;
                ParametrosPessoas.Personalidade pers = pessoa.personalidade();
                regra(pers != null, arq, nome + ": inspetor precisa de personalidade");
                if (pers != null) {
                    regra(entre0e100(pers.rigor()) && entre0e100(pers.velocidade()) && entre0e100(pers.paciencia()),
                            arq, nome + ".personalidade: atributos devem estar entre 0 e 100");
                }
            }
        }
        regra(produtivos >= 1, arq, "a equipe precisa de pelo menos 1 pessoa de PRODUCAO");
        regra(inspetores == 1, arq, "o MVP tem exatamente 1 INSPETOR; veio " + inspetores);
    }

    private void fornecedores(ParametrosFornecedores f) {
        String arq = CarregadorDados.FORNECEDORES;
        regra(f.pesosScorecard() != null && f.pesosScorecard().soma() == 100, arq, "pesosScorecard deve somar 100");
        probabilidade(arq, "taxaAdministrativaChargeback", f.taxaAdministrativaChargeback());
        regra(f.prazo8dDiasUteis() >= 1, arq, "prazo8dDiasUteis deve ser pelo menos 1");
        regra(f.diasAtrasoMin() >= 0 && f.diasAtrasoMin() <= f.diasAtrasoMax(), arq, "diasAtrasoMin deve ser <= diasAtrasoMax");
        regra(f.ppmReferencia() > 0, arq, "ppmReferencia deve ser maior que zero");
        positivo(arq, "fatorFornecedorMin", f.fatorFornecedorMin());
        regra(f.fatorFornecedorMax() != null && f.fatorFornecedorMin() != null
                && f.fatorFornecedorMax().compareTo(f.fatorFornecedorMin()) >= 0, arq, "fatorFornecedorMax deve ser >= fatorFornecedorMin");
        regra(f.notas().keySet().containsAll(List.of("A", "B", "C", "D", "E")), arq, "notas deve ter os cortes de A a E");
        f.perfis().forEach((perfil, pp) -> {
            probabilidade(arq, "perfis." + perfil + ".probabilidadeAtraso", pp.probabilidadeAtraso());
            positivo(arq, "perfis." + perfil + ".fatorPreco", pp.fatorPreco());
            regra(pp.ppmBase() >= 0 && pp.ppmBase() <= 1_000_000, arq, "perfis." + perfil + ".ppmBase fora de 0..1.000.000");
        });
        Set<String> ids = new HashSet<>();
        for (ParametrosFornecedores.FornecedorInicial forn : f.fornecedores()) {
            regra(ids.add(forn.id()), arq, "id repetido " + forn.id());
            regra(forn.perfil() != null && f.perfis().containsKey(forn.perfil()), arq,
                    "fornecedores[" + forn.id() + "]: perfil sem parametros: " + forn.perfil());
        }
        regra(!f.fornecedores().isEmpty(), arq, "fornecedores nao pode ser vazio");
    }

    private void eventos(ParametrosEventos e) {
        String arq = CarregadorDados.EVENTOS;
        regra(e.quebraPeca() != null && e.acidente() != null && e.demissao() != null, arq,
                "quebraPeca, acidente e demissao sao obrigatorios");
        if (e.quebraPeca() != null) {
            probabilidade(arq, "quebraPeca.probabilidadePorGate", e.quebraPeca().probabilidadePorGate());
        }
        if (e.acidente() != null) {
            probabilidade(arq, "acidente.probabilidadePorHh", e.acidente().probabilidadePorHh());
            regra(e.acidente().diasPerdidosMin() >= 0 && e.acidente().diasPerdidosMin() <= e.acidente().diasPerdidosMax(),
                    arq, "acidente: diasPerdidosMin deve ser <= diasPerdidosMax");
        }
        if (e.demissao() != null) {
            probabilidade(arq, "demissao.probabilidadeDiariaBase", e.demissao().probabilidadeDiariaBase());
            probabilidade(arq, "demissao.probabilidadeDiariaMoralBaixa", e.demissao().probabilidadeDiariaMoralBaixa());
        }
    }

    private void financeiro(ParametrosFinanceiro f) {
        String arq = CarregadorDados.FINANCEIRO;
        naoNegativo(arq, "caixaInicial", f.caixaInicial());
        naoNegativo(arq, "despesasFixasMensais", f.despesasFixasMensais());
        positivo(arq, "multiploValuation", f.multiploValuation());
        probabilidade(arq, "descontoPorAcidenteComAfastamento", f.descontoPorAcidenteComAfastamento());
        probabilidade(arq, "descontoPorNcEscapada", f.descontoPorNcEscapada());
        regra(f.mesesCaixaNegativoParaDerrota() >= 1, arq, "mesesCaixaNegativoParaDerrota deve ser pelo menos 1");
        probabilidade(arq, "encargosSobreSalarios", f.encargosSobreSalarios());
        regra(f.conselho() != null, arq, "conselho e obrigatorio");
        if (f.conselho() != null) {
            regra(f.conselho().pacienciaInicial() >= 1 && f.conselho().pacienciaInicial() <= f.conselho().pacienciaMaxima(),
                    arq, "conselho.pacienciaInicial deve estar entre 1 e pacienciaMaxima");
            naoNegativo(arq, "conselho.metaCaixaMinimo", f.conselho().metaCaixaMinimo());
            positivo(arq, "conselho.fatorMaterialDemais", f.conselho().fatorMaterialDemais());
        }
        regra(f.decisoes().keySet().containsAll(br.marellar.engine.regras.ConselhoRegra.OPCOES), arq,
                "decisoes precisa do custo de " + br.marellar.engine.regras.ConselhoRegra.OPCOES);
        f.decisoes().forEach((id, custo) -> naoNegativo(arq, "decisoes." + id, custo));
        regra(f.kitsAntecipados() >= 1, arq, "kitsAntecipados deve ser pelo menos 1");
    }

    private void textos(Textos t) {
        String arq = CarregadorDados.TEXTOS;
        for (String caminho : List.of("inspetor.ncDetectada", "inspetor.gateAprovado", "inspetor.concessaoDemais",
                "conselheiros", "metas")) {
            regra(t.no(caminho).isArray() && !t.no(caminho).isEmpty(), arq, caminho + " deve ser uma lista nao vazia");
        }
        for (String caminho : List.of("eventos.gateConcluido", "eventos.gateAprovado", "eventos.barcoEntregue",
                "eventos.fimDoMes", "eventos.ncDetectada", "eventos.ncCritica", "eventos.retrabalhoConcluido",
                "eventos.concessao", "eventos.rncAberta", "eventos.paradoMaterial", "eventos.materialChegou",
                "eventos.pedidoFeito", "eventos.quebraPeca", "eventos.acidente", "eventos.demissao",
                "eventos.contratacao", "eventos.chegadaContratado", "eventos.treinamentoInicio",
                "eventos.treinamentoFim", "eventos.subiuNivel", "eventos.retornoAfastamento", "eventos.umAUm",
                "eventos.oitoDRespondido", "eventos.oitoDVencido", "eventos.scorecard", "eventos.cotaRecebida",
                "eventos.salarios", "eventos.decisaoConselho", "eventos.derrota",
                "conselho.serio.acidente", "conselho.serio.caixaRuim", "conselho.serio.metaPerdida",
                "conselho.serio.metaCumprida", "conselho.serio.ebitdaNegativo", "conselho.serio.caixaNegativo", "conselho.humor.materialDemais",
                "conselho.humor.tudoBem", "conselho.humor.zeroNc", "conselho.derrota.CAIXA", "conselho.derrota.CONSELHO")) {
            regra(t.no(caminho).isTextual(), arq, caminho + " ausente");
        }
    }

    // ----- utilitarios -----

    private void regra(boolean ok, String arquivo, String mensagem) {
        if (!ok) {
            erros.add(arquivo + ": " + mensagem);
        }
    }

    private void positivo(String arq, String campo, BigDecimal valor) {
        regra(valor != null && valor.signum() > 0, arq, campo + " deve ser maior que zero");
    }

    private void naoNegativo(String arq, String campo, BigDecimal valor) {
        regra(valor != null && valor.signum() >= 0, arq, campo + " nao pode ser negativo");
    }

    private void percentual(String arq, String campo, BigDecimal valor) {
        regra(valor != null && valor.signum() >= 0 && valor.compareTo(CEM) <= 0, arq, campo + " deve estar entre 0 e 100");
    }

    private void probabilidade(String arq, String campo, BigDecimal valor) {
        regra(valor != null && valor.signum() >= 0 && valor.compareTo(BigDecimal.ONE) <= 0, arq, campo + " deve estar entre 0 e 1");
    }

    private static boolean entre0e100(int v) {
        return v >= 0 && v <= 100;
    }

    private static BigDecimal nulo(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static <T> BigDecimal soma(List<T> itens, Function<T, BigDecimal> campo) {
        return itens.stream().map(campo).map(ValidadorDados::nulo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
