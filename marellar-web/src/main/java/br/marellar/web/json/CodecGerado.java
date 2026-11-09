package br.marellar.web.json;

import java.util.Map;

/**
 * GERADO por GeradorCodec (src/test). Nao edite a mao: mude o record e rode
 * {@code mvnw -pl marellar-web test -Dtest=CodecEmDiaTest -Dmarellar.gerarCodec=true}.
 * Leitores e escritores de JSON sem reflexao, no mesmo formato do Jackson da API.
 */
@SuppressWarnings({"unused", "PMD"})
public final class CodecGerado {

    private CodecGerado() {
    }

    public static br.marellar.engine.dados.ParametrosLinha lerParametrosLinha(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "ParametrosLinha");
        return new br.marellar.engine.dados.ParametrosLinha(
                lerLinha(m.get("linha")),
                Json.texto(m.get("nome")),
                Json.decimal(m.get("hhPorBarco")),
                Json.decimal(m.get("custoHh")),
                Json.decimal(m.get("materialPorBarco")),
                Json.decimal(m.get("precoVenda")),
                Json.decimal(m.get("percentualConsumiveis")),
                Json.decimal(m.get("percentualImportados")),
                Json.inteiro(m.get("prazoRecebimentoDias")),
                Json.decimal(m.get("cotaContratoPercentual")),
                Json.inteiro(m.get("maxPessoasPorBarco")),
                Json.inteiro(m.get("ticksInspecao")),
                Json.inteiro(m.get("estoqueInicialKits")),
                Json.inteiro(m.get("estoqueMinimoKits")),
                Json.inteiro(m.get("pecasPorKit")),
                Json.lista(m.get("gates"), o -> lerParametrosLinha_Gate(o)),
                Json.lista(m.get("barcosIniciais"), o -> lerParametrosLinha_BarcoInicial(o)));
    }

    public static void escreverParametrosLinha(StringBuilder sb, br.marellar.engine.dados.ParametrosLinha v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"linha\":");
        ((Json.Escritor<br.marellar.engine.dominio.Linha>) CodecGerado::escreverLinha).escrever(sb, v.linha());
        sb.append(",\"nome\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.nome());
        sb.append(",\"hhPorBarco\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhPorBarco());
        sb.append(",\"custoHh\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.custoHh());
        sb.append(",\"materialPorBarco\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.materialPorBarco());
        sb.append(",\"precoVenda\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.precoVenda());
        sb.append(",\"percentualConsumiveis\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.percentualConsumiveis());
        sb.append(",\"percentualImportados\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.percentualImportados());
        sb.append(",\"prazoRecebimentoDias\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.prazoRecebimentoDias());
        sb.append(",\"cotaContratoPercentual\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.cotaContratoPercentual());
        sb.append(",\"maxPessoasPorBarco\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.maxPessoasPorBarco());
        sb.append(",\"ticksInspecao\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.ticksInspecao());
        sb.append(",\"estoqueInicialKits\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.estoqueInicialKits());
        sb.append(",\"estoqueMinimoKits\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.estoqueMinimoKits());
        sb.append(",\"pecasPorKit\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.pecasPorKit());
        sb.append(",\"gates\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.dados.ParametrosLinha.Gate>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.dados.ParametrosLinha.Gate>) CodecGerado::escreverParametrosLinha_Gate)).escrever(sb, v.gates());
        sb.append(",\"barcosIniciais\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.dados.ParametrosLinha.BarcoInicial>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.dados.ParametrosLinha.BarcoInicial>) CodecGerado::escreverParametrosLinha_BarcoInicial)).escrever(sb, v.barcosIniciais());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.Linha lerLinha(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.Linha.valueOf(s);
    }

    public static void escreverLinha(StringBuilder sb, br.marellar.engine.dominio.Linha v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.dados.ParametrosLinha.Gate lerParametrosLinha_Gate(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Gate");
        return new br.marellar.engine.dados.ParametrosLinha.Gate(
                lerCodigoGate(m.get("codigo")),
                Json.texto(m.get("nome")),
                Json.decimal(m.get("pesoHhPercentual")),
                Json.decimal(m.get("cotaPercentual")),
                Json.texto(m.get("item")),
                Json.decimal(m.get("materialPercentual")));
    }

    public static void escreverParametrosLinha_Gate(StringBuilder sb, br.marellar.engine.dados.ParametrosLinha.Gate v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"codigo\":");
        ((Json.Escritor<br.marellar.engine.dominio.CodigoGate>) CodecGerado::escreverCodigoGate).escrever(sb, v.codigo());
        sb.append(",\"nome\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.nome());
        sb.append(",\"pesoHhPercentual\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.pesoHhPercentual());
        sb.append(",\"cotaPercentual\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.cotaPercentual());
        sb.append(",\"item\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.item());
        sb.append(",\"materialPercentual\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.materialPercentual());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.CodigoGate lerCodigoGate(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.CodigoGate.valueOf(s);
    }

    public static void escreverCodigoGate(StringBuilder sb, br.marellar.engine.dominio.CodigoGate v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.dados.ParametrosLinha.BarcoInicial lerParametrosLinha_BarcoInicial(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "BarcoInicial");
        return new br.marellar.engine.dados.ParametrosLinha.BarcoInicial(
                Json.texto(m.get("id")),
                Json.texto(m.get("cliente")),
                lerCodigoGate(m.get("gateInicial")));
    }

    public static void escreverParametrosLinha_BarcoInicial(StringBuilder sb, br.marellar.engine.dados.ParametrosLinha.BarcoInicial v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"cliente\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.cliente());
        sb.append(",\"gateInicial\":");
        ((Json.Escritor<br.marellar.engine.dominio.CodigoGate>) CodecGerado::escreverCodigoGate).escrever(sb, v.gateInicial());
        sb.append("}");
    }

    public static br.marellar.engine.dados.CatalogoNc lerCatalogoNc(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "CatalogoNc");
        return new br.marellar.engine.dados.CatalogoNc(
                Json.mapa(m.get("deteccaoBase"), k -> br.marellar.engine.dominio.Gravidade.valueOf(k), o -> Json.decimal(o)),
                Json.inteiro(m.get("maxNcsPorGate")),
                Json.mapa(m.get("fatorNivelEquipe"), k -> Integer.valueOf(k), o -> Json.decimal(o)),
                lerCatalogoNc_Inspecao(m.get("inspecao")),
                Json.lista(m.get("tipos"), o -> lerCatalogoNc_TipoNc(o)));
    }

    public static void escreverCatalogoNc(StringBuilder sb, br.marellar.engine.dados.CatalogoNc v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"deteccaoBase\":");
        ((Json.Escritor<java.util.Map<br.marellar.engine.dominio.Gravidade, java.math.BigDecimal>>) (s, x) -> Json.mapa(s, x, (Json.Escritor<java.math.BigDecimal>) Json::decimal)).escrever(sb, v.deteccaoBase());
        sb.append(",\"maxNcsPorGate\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.maxNcsPorGate());
        sb.append(",\"fatorNivelEquipe\":");
        ((Json.Escritor<java.util.Map<java.lang.Integer, java.math.BigDecimal>>) (s, x) -> Json.mapa(s, x, (Json.Escritor<java.math.BigDecimal>) Json::decimal)).escrever(sb, v.fatorNivelEquipe());
        sb.append(",\"inspecao\":");
        ((Json.Escritor<br.marellar.engine.dados.CatalogoNc.Inspecao>) CodecGerado::escreverCatalogoNc_Inspecao).escrever(sb, v.inspecao());
        sb.append(",\"tipos\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.dados.CatalogoNc.TipoNc>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.dados.CatalogoNc.TipoNc>) CodecGerado::escreverCatalogoNc_TipoNc)).escrever(sb, v.tipos());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.Gravidade lerGravidade(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.Gravidade.valueOf(s);
    }

    public static void escreverGravidade(StringBuilder sb, br.marellar.engine.dominio.Gravidade v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.dados.CatalogoNc.Inspecao lerCatalogoNc_Inspecao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Inspecao");
        return new br.marellar.engine.dados.CatalogoNc.Inspecao(
                Json.decimal(m.get("deteccaoBase")),
                Json.decimal(m.get("pesoRigorNaDeteccao")),
                Json.decimal(m.get("pesoVelocidadeNaDeteccao")),
                Json.decimal(m.get("pesoRigorNoTempo")),
                Json.decimal(m.get("pesoVelocidadeNoTempo")),
                Json.inteiro(m.get("filaSemPerda")),
                Json.decimal(m.get("perdaPorBarcoNaFila")),
                Json.decimal(m.get("fatorCargaMinimo")),
                Json.decimal(m.get("quedaMoralPorConcessao")),
                Json.inteiro(m.get("concessoesParaReclamar")));
    }

    public static void escreverCatalogoNc_Inspecao(StringBuilder sb, br.marellar.engine.dados.CatalogoNc.Inspecao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"deteccaoBase\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.deteccaoBase());
        sb.append(",\"pesoRigorNaDeteccao\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.pesoRigorNaDeteccao());
        sb.append(",\"pesoVelocidadeNaDeteccao\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.pesoVelocidadeNaDeteccao());
        sb.append(",\"pesoRigorNoTempo\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.pesoRigorNoTempo());
        sb.append(",\"pesoVelocidadeNoTempo\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.pesoVelocidadeNoTempo());
        sb.append(",\"filaSemPerda\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.filaSemPerda());
        sb.append(",\"perdaPorBarcoNaFila\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.perdaPorBarcoNaFila());
        sb.append(",\"fatorCargaMinimo\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.fatorCargaMinimo());
        sb.append(",\"quedaMoralPorConcessao\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.quedaMoralPorConcessao());
        sb.append(",\"concessoesParaReclamar\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.concessoesParaReclamar());
        sb.append("}");
    }

    public static br.marellar.engine.dados.CatalogoNc.TipoNc lerCatalogoNc_TipoNc(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "TipoNc");
        return new br.marellar.engine.dados.CatalogoNc.TipoNc(
                Json.texto(m.get("codigo")),
                Json.texto(m.get("descricao")),
                lerGravidade(m.get("gravidade")),
                lerOrigem(m.get("origem")),
                Json.lista(m.get("gates"), o -> lerCodigoGate(o)),
                Json.decimal(m.get("probabilidade")),
                Json.decimal(m.get("hhRetrabalho")),
                Json.texto(m.get("item")),
                Json.decimal(m.get("custoMaterial")));
    }

    public static void escreverCatalogoNc_TipoNc(StringBuilder sb, br.marellar.engine.dados.CatalogoNc.TipoNc v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"codigo\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.codigo());
        sb.append(",\"descricao\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.descricao());
        sb.append(",\"gravidade\":");
        ((Json.Escritor<br.marellar.engine.dominio.Gravidade>) CodecGerado::escreverGravidade).escrever(sb, v.gravidade());
        sb.append(",\"origem\":");
        ((Json.Escritor<br.marellar.engine.dominio.Origem>) CodecGerado::escreverOrigem).escrever(sb, v.origem());
        sb.append(",\"gates\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.dominio.CodigoGate>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.dominio.CodigoGate>) CodecGerado::escreverCodigoGate)).escrever(sb, v.gates());
        sb.append(",\"probabilidade\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.probabilidade());
        sb.append(",\"hhRetrabalho\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhRetrabalho());
        sb.append(",\"item\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.item());
        sb.append(",\"custoMaterial\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.custoMaterial());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.Origem lerOrigem(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.Origem.valueOf(s);
    }

    public static void escreverOrigem(StringBuilder sb, br.marellar.engine.dominio.Origem v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.dados.ParametrosPessoas lerParametrosPessoas(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "ParametrosPessoas");
        return new br.marellar.engine.dados.ParametrosPessoas(
                Json.mapa(m.get("fatorNivel"), k -> Integer.valueOf(k), o -> Json.decimal(o)),
                Json.mapa(m.get("salarioPorNivel"), k -> Integer.valueOf(k), o -> Json.decimal(o)),
                Json.decimal(m.get("salarioInspetor")),
                Json.inteiro(m.get("diasChegadaContratacao")),
                Json.inteiro(m.get("diasParaSubirNivel")),
                Json.inteiro(m.get("nivelMaximoPorTempo")),
                Json.inteiro(m.get("diasTreinamento")),
                Json.inteiro(m.get("ganhoMoralUmAUm")),
                Json.decimal(m.get("horasLiderUmAUm")),
                Json.inteiro(m.get("diasSemUmAUmAntesDeCair")),
                Json.inteiro(m.get("quedaMoralDiariaSemUmAUm")),
                Json.inteiro(m.get("diasEntreUmAUm")),
                Json.inteiro(m.get("moralInicialContratado")),
                Json.lista(m.get("nomesParaContratar"), o -> Json.texto(o)),
                Json.lista(m.get("equipe"), o -> lerParametrosPessoas_PessoaInicial(o)));
    }

    public static void escreverParametrosPessoas(StringBuilder sb, br.marellar.engine.dados.ParametrosPessoas v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"fatorNivel\":");
        ((Json.Escritor<java.util.Map<java.lang.Integer, java.math.BigDecimal>>) (s, x) -> Json.mapa(s, x, (Json.Escritor<java.math.BigDecimal>) Json::decimal)).escrever(sb, v.fatorNivel());
        sb.append(",\"salarioPorNivel\":");
        ((Json.Escritor<java.util.Map<java.lang.Integer, java.math.BigDecimal>>) (s, x) -> Json.mapa(s, x, (Json.Escritor<java.math.BigDecimal>) Json::decimal)).escrever(sb, v.salarioPorNivel());
        sb.append(",\"salarioInspetor\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.salarioInspetor());
        sb.append(",\"diasChegadaContratacao\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasChegadaContratacao());
        sb.append(",\"diasParaSubirNivel\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasParaSubirNivel());
        sb.append(",\"nivelMaximoPorTempo\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.nivelMaximoPorTempo());
        sb.append(",\"diasTreinamento\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasTreinamento());
        sb.append(",\"ganhoMoralUmAUm\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.ganhoMoralUmAUm());
        sb.append(",\"horasLiderUmAUm\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.horasLiderUmAUm());
        sb.append(",\"diasSemUmAUmAntesDeCair\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasSemUmAUmAntesDeCair());
        sb.append(",\"quedaMoralDiariaSemUmAUm\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.quedaMoralDiariaSemUmAUm());
        sb.append(",\"diasEntreUmAUm\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasEntreUmAUm());
        sb.append(",\"moralInicialContratado\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.moralInicialContratado());
        sb.append(",\"nomesParaContratar\":");
        ((Json.Escritor<java.util.List<java.lang.String>>) (s, x) -> Json.lista(s, x, (Json.Escritor<java.lang.String>) Json::texto)).escrever(sb, v.nomesParaContratar());
        sb.append(",\"equipe\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.dados.ParametrosPessoas.PessoaInicial>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.dados.ParametrosPessoas.PessoaInicial>) CodecGerado::escreverParametrosPessoas_PessoaInicial)).escrever(sb, v.equipe());
        sb.append("}");
    }

    public static br.marellar.engine.dados.ParametrosPessoas.PessoaInicial lerParametrosPessoas_PessoaInicial(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "PessoaInicial");
        return new br.marellar.engine.dados.ParametrosPessoas.PessoaInicial(
                Json.texto(m.get("id")),
                Json.texto(m.get("nome")),
                lerFuncao(m.get("funcao")),
                Json.inteiro(m.get("nivel")),
                Json.inteiro(m.get("moral")),
                lerParametrosPessoas_Personalidade(m.get("personalidade")),
                Json.logicoOuNulo(m.get("treinoSegurancaEmDia")));
    }

    public static void escreverParametrosPessoas_PessoaInicial(StringBuilder sb, br.marellar.engine.dados.ParametrosPessoas.PessoaInicial v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"nome\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.nome());
        sb.append(",\"funcao\":");
        ((Json.Escritor<br.marellar.engine.dominio.Funcao>) CodecGerado::escreverFuncao).escrever(sb, v.funcao());
        sb.append(",\"nivel\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.nivel());
        sb.append(",\"moral\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.moral());
        sb.append(",\"personalidade\":");
        ((Json.Escritor<br.marellar.engine.dados.ParametrosPessoas.Personalidade>) CodecGerado::escreverParametrosPessoas_Personalidade).escrever(sb, v.personalidade());
        sb.append(",\"treinoSegurancaEmDia\":");
        ((Json.Escritor<java.lang.Boolean>) Json::numero).escrever(sb, v.treinoSegurancaEmDia());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.Funcao lerFuncao(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.Funcao.valueOf(s);
    }

    public static void escreverFuncao(StringBuilder sb, br.marellar.engine.dominio.Funcao v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.dados.ParametrosPessoas.Personalidade lerParametrosPessoas_Personalidade(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Personalidade");
        return new br.marellar.engine.dados.ParametrosPessoas.Personalidade(
                Json.inteiro(m.get("rigor")),
                Json.inteiro(m.get("velocidade")),
                Json.inteiro(m.get("paciencia")));
    }

    public static void escreverParametrosPessoas_Personalidade(StringBuilder sb, br.marellar.engine.dados.ParametrosPessoas.Personalidade v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"rigor\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.rigor());
        sb.append(",\"velocidade\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.velocidade());
        sb.append(",\"paciencia\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.paciencia());
        sb.append("}");
    }

    public static br.marellar.engine.dados.ParametrosFornecedores lerParametrosFornecedores(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "ParametrosFornecedores");
        return new br.marellar.engine.dados.ParametrosFornecedores(
                lerParametrosFornecedores_PesosScorecard(m.get("pesosScorecard")),
                Json.decimal(m.get("taxaAdministrativaChargeback")),
                Json.inteiro(m.get("prazo8dDiasUteis")),
                Json.inteiro(m.get("diasAtrasoMin")),
                Json.inteiro(m.get("diasAtrasoMax")),
                Json.inteiro(m.get("ppmReferencia")),
                Json.decimal(m.get("fatorFornecedorMin")),
                Json.decimal(m.get("fatorFornecedorMax")),
                Json.mapa(m.get("notas"), k -> k, o -> Json.inteiroOuNulo(o)),
                Json.mapa(m.get("perfis"), k -> br.marellar.engine.dominio.PerfilFornecedor.valueOf(k), o -> lerParametrosFornecedores_ParametrosPerfil(o)),
                Json.lista(m.get("fornecedores"), o -> lerParametrosFornecedores_FornecedorInicial(o)));
    }

    public static void escreverParametrosFornecedores(StringBuilder sb, br.marellar.engine.dados.ParametrosFornecedores v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"pesosScorecard\":");
        ((Json.Escritor<br.marellar.engine.dados.ParametrosFornecedores.PesosScorecard>) CodecGerado::escreverParametrosFornecedores_PesosScorecard).escrever(sb, v.pesosScorecard());
        sb.append(",\"taxaAdministrativaChargeback\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.taxaAdministrativaChargeback());
        sb.append(",\"prazo8dDiasUteis\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.prazo8dDiasUteis());
        sb.append(",\"diasAtrasoMin\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasAtrasoMin());
        sb.append(",\"diasAtrasoMax\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasAtrasoMax());
        sb.append(",\"ppmReferencia\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.ppmReferencia());
        sb.append(",\"fatorFornecedorMin\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.fatorFornecedorMin());
        sb.append(",\"fatorFornecedorMax\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.fatorFornecedorMax());
        sb.append(",\"notas\":");
        ((Json.Escritor<java.util.Map<java.lang.String, java.lang.Integer>>) (s, x) -> Json.mapa(s, x, (Json.Escritor<java.lang.Integer>) Json::numero)).escrever(sb, v.notas());
        sb.append(",\"perfis\":");
        ((Json.Escritor<java.util.Map<br.marellar.engine.dominio.PerfilFornecedor, br.marellar.engine.dados.ParametrosFornecedores.ParametrosPerfil>>) (s, x) -> Json.mapa(s, x, (Json.Escritor<br.marellar.engine.dados.ParametrosFornecedores.ParametrosPerfil>) CodecGerado::escreverParametrosFornecedores_ParametrosPerfil)).escrever(sb, v.perfis());
        sb.append(",\"fornecedores\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.dados.ParametrosFornecedores.FornecedorInicial>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.dados.ParametrosFornecedores.FornecedorInicial>) CodecGerado::escreverParametrosFornecedores_FornecedorInicial)).escrever(sb, v.fornecedores());
        sb.append("}");
    }

    public static br.marellar.engine.dados.ParametrosFornecedores.PesosScorecard lerParametrosFornecedores_PesosScorecard(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "PesosScorecard");
        return new br.marellar.engine.dados.ParametrosFornecedores.PesosScorecard(
                Json.inteiro(m.get("entrega")),
                Json.inteiro(m.get("qualidade")),
                Json.inteiro(m.get("custo")),
                Json.inteiro(m.get("resposta")));
    }

    public static void escreverParametrosFornecedores_PesosScorecard(StringBuilder sb, br.marellar.engine.dados.ParametrosFornecedores.PesosScorecard v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"entrega\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.entrega());
        sb.append(",\"qualidade\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.qualidade());
        sb.append(",\"custo\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.custo());
        sb.append(",\"resposta\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.resposta());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.PerfilFornecedor lerPerfilFornecedor(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.PerfilFornecedor.valueOf(s);
    }

    public static void escreverPerfilFornecedor(StringBuilder sb, br.marellar.engine.dominio.PerfilFornecedor v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.dados.ParametrosFornecedores.ParametrosPerfil lerParametrosFornecedores_ParametrosPerfil(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "ParametrosPerfil");
        return new br.marellar.engine.dados.ParametrosFornecedores.ParametrosPerfil(
                Json.inteiro(m.get("ppmBase")),
                Json.decimal(m.get("probabilidadeAtraso")),
                Json.inteiro(m.get("diasEntrega")),
                Json.decimal(m.get("fatorPreco")),
                Json.inteiro(m.get("diasResposta8d")));
    }

    public static void escreverParametrosFornecedores_ParametrosPerfil(StringBuilder sb, br.marellar.engine.dados.ParametrosFornecedores.ParametrosPerfil v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"ppmBase\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.ppmBase());
        sb.append(",\"probabilidadeAtraso\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.probabilidadeAtraso());
        sb.append(",\"diasEntrega\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasEntrega());
        sb.append(",\"fatorPreco\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.fatorPreco());
        sb.append(",\"diasResposta8d\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasResposta8d());
        sb.append("}");
    }

    public static br.marellar.engine.dados.ParametrosFornecedores.FornecedorInicial lerParametrosFornecedores_FornecedorInicial(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "FornecedorInicial");
        return new br.marellar.engine.dados.ParametrosFornecedores.FornecedorInicial(
                Json.texto(m.get("id")),
                Json.texto(m.get("nome")),
                lerPerfilFornecedor(m.get("perfil")),
                Json.lista(m.get("itens"), o -> Json.texto(o)));
    }

    public static void escreverParametrosFornecedores_FornecedorInicial(StringBuilder sb, br.marellar.engine.dados.ParametrosFornecedores.FornecedorInicial v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"nome\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.nome());
        sb.append(",\"perfil\":");
        ((Json.Escritor<br.marellar.engine.dominio.PerfilFornecedor>) CodecGerado::escreverPerfilFornecedor).escrever(sb, v.perfil());
        sb.append(",\"itens\":");
        ((Json.Escritor<java.util.List<java.lang.String>>) (s, x) -> Json.lista(s, x, (Json.Escritor<java.lang.String>) Json::texto)).escrever(sb, v.itens());
        sb.append("}");
    }

    public static br.marellar.engine.dados.ParametrosEventos lerParametrosEventos(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "ParametrosEventos");
        return new br.marellar.engine.dados.ParametrosEventos(
                lerParametrosEventos_QuebraPeca(m.get("quebraPeca")),
                lerParametrosEventos_Acidente(m.get("acidente")),
                lerParametrosEventos_Demissao(m.get("demissao")));
    }

    public static void escreverParametrosEventos(StringBuilder sb, br.marellar.engine.dados.ParametrosEventos v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"quebraPeca\":");
        ((Json.Escritor<br.marellar.engine.dados.ParametrosEventos.QuebraPeca>) CodecGerado::escreverParametrosEventos_QuebraPeca).escrever(sb, v.quebraPeca());
        sb.append(",\"acidente\":");
        ((Json.Escritor<br.marellar.engine.dados.ParametrosEventos.Acidente>) CodecGerado::escreverParametrosEventos_Acidente).escrever(sb, v.acidente());
        sb.append(",\"demissao\":");
        ((Json.Escritor<br.marellar.engine.dados.ParametrosEventos.Demissao>) CodecGerado::escreverParametrosEventos_Demissao).escrever(sb, v.demissao());
        sb.append("}");
    }

    public static br.marellar.engine.dados.ParametrosEventos.QuebraPeca lerParametrosEventos_QuebraPeca(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "QuebraPeca");
        return new br.marellar.engine.dados.ParametrosEventos.QuebraPeca(
                Json.decimal(m.get("probabilidadePorGate")),
                Json.decimal(m.get("multiplicadorPressa")),
                Json.decimal(m.get("multiplicadorMoralBaixa")),
                Json.decimal(m.get("hhRetrabalho")),
                Json.decimal(m.get("custoMaterial")));
    }

    public static void escreverParametrosEventos_QuebraPeca(StringBuilder sb, br.marellar.engine.dados.ParametrosEventos.QuebraPeca v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"probabilidadePorGate\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.probabilidadePorGate());
        sb.append(",\"multiplicadorPressa\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.multiplicadorPressa());
        sb.append(",\"multiplicadorMoralBaixa\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.multiplicadorMoralBaixa());
        sb.append(",\"hhRetrabalho\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhRetrabalho());
        sb.append(",\"custoMaterial\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.custoMaterial());
        sb.append("}");
    }

    public static br.marellar.engine.dados.ParametrosEventos.Acidente lerParametrosEventos_Acidente(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Acidente");
        return new br.marellar.engine.dados.ParametrosEventos.Acidente(
                Json.decimal(m.get("probabilidadePorHh")),
                Json.decimal(m.get("multiplicadorTreinoVencido")),
                Json.decimal(m.get("multiplicadorSobrecarga")),
                Json.inteiro(m.get("diasPerdidosMin")),
                Json.inteiro(m.get("diasPerdidosMax")),
                Json.inteiro(m.get("quedaMoralEquipe")));
    }

    public static void escreverParametrosEventos_Acidente(StringBuilder sb, br.marellar.engine.dados.ParametrosEventos.Acidente v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"probabilidadePorHh\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.probabilidadePorHh());
        sb.append(",\"multiplicadorTreinoVencido\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.multiplicadorTreinoVencido());
        sb.append(",\"multiplicadorSobrecarga\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.multiplicadorSobrecarga());
        sb.append(",\"diasPerdidosMin\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasPerdidosMin());
        sb.append(",\"diasPerdidosMax\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasPerdidosMax());
        sb.append(",\"quedaMoralEquipe\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.quedaMoralEquipe());
        sb.append("}");
    }

    public static br.marellar.engine.dados.ParametrosEventos.Demissao lerParametrosEventos_Demissao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Demissao");
        return new br.marellar.engine.dados.ParametrosEventos.Demissao(
                Json.decimal(m.get("probabilidadeDiariaBase")),
                Json.decimal(m.get("probabilidadeDiariaMoralBaixa")),
                Json.inteiro(m.get("limiarMoralBaixa")));
    }

    public static void escreverParametrosEventos_Demissao(StringBuilder sb, br.marellar.engine.dados.ParametrosEventos.Demissao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"probabilidadeDiariaBase\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.probabilidadeDiariaBase());
        sb.append(",\"probabilidadeDiariaMoralBaixa\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.probabilidadeDiariaMoralBaixa());
        sb.append(",\"limiarMoralBaixa\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.limiarMoralBaixa());
        sb.append("}");
    }

    public static br.marellar.engine.dados.ParametrosFinanceiro lerParametrosFinanceiro(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "ParametrosFinanceiro");
        return new br.marellar.engine.dados.ParametrosFinanceiro(
                Json.decimal(m.get("caixaInicial")),
                Json.decimal(m.get("despesasFixasMensais")),
                Json.decimal(m.get("encargosSobreSalarios")),
                Json.decimal(m.get("multiploValuation")),
                Json.decimal(m.get("descontoPorAcidenteComAfastamento")),
                Json.decimal(m.get("descontoPorNcEscapada")),
                Json.inteiro(m.get("mesesCaixaNegativoParaDerrota")),
                lerParametrosFinanceiro_Conselho(m.get("conselho")),
                Json.mapa(m.get("decisoes"), k -> k, o -> Json.decimal(o)),
                Json.inteiro(m.get("kitsAntecipados")),
                Json.inteiro(m.get("ganhoMoralEncontro")));
    }

    public static void escreverParametrosFinanceiro(StringBuilder sb, br.marellar.engine.dados.ParametrosFinanceiro v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"caixaInicial\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.caixaInicial());
        sb.append(",\"despesasFixasMensais\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.despesasFixasMensais());
        sb.append(",\"encargosSobreSalarios\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.encargosSobreSalarios());
        sb.append(",\"multiploValuation\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.multiploValuation());
        sb.append(",\"descontoPorAcidenteComAfastamento\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.descontoPorAcidenteComAfastamento());
        sb.append(",\"descontoPorNcEscapada\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.descontoPorNcEscapada());
        sb.append(",\"mesesCaixaNegativoParaDerrota\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.mesesCaixaNegativoParaDerrota());
        sb.append(",\"conselho\":");
        ((Json.Escritor<br.marellar.engine.dados.ParametrosFinanceiro.Conselho>) CodecGerado::escreverParametrosFinanceiro_Conselho).escrever(sb, v.conselho());
        sb.append(",\"decisoes\":");
        ((Json.Escritor<java.util.Map<java.lang.String, java.math.BigDecimal>>) (s, x) -> Json.mapa(s, x, (Json.Escritor<java.math.BigDecimal>) Json::decimal)).escrever(sb, v.decisoes());
        sb.append(",\"kitsAntecipados\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.kitsAntecipados());
        sb.append(",\"ganhoMoralEncontro\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.ganhoMoralEncontro());
        sb.append("}");
    }

    public static br.marellar.engine.dados.ParametrosFinanceiro.Conselho lerParametrosFinanceiro_Conselho(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Conselho");
        return new br.marellar.engine.dados.ParametrosFinanceiro.Conselho(
                Json.inteiro(m.get("pacienciaInicial")),
                Json.inteiro(m.get("pacienciaMaxima")),
                Json.inteiro(m.get("metaCascosDm")),
                Json.decimal(m.get("metaCaixaMinimo")),
                Json.inteiro(m.get("metaNcPorBarcoMaxima")),
                Json.decimal(m.get("fatorMaterialDemais")));
    }

    public static void escreverParametrosFinanceiro_Conselho(StringBuilder sb, br.marellar.engine.dados.ParametrosFinanceiro.Conselho v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"pacienciaInicial\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.pacienciaInicial());
        sb.append(",\"pacienciaMaxima\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.pacienciaMaxima());
        sb.append(",\"metaCascosDm\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.metaCascosDm());
        sb.append(",\"metaCaixaMinimo\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.metaCaixaMinimo());
        sb.append(",\"metaNcPorBarcoMaxima\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.metaNcPorBarcoMaxima());
        sb.append(",\"fatorMaterialDemais\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.fatorMaterialDemais());
        sb.append("}");
    }

    public static br.marellar.engine.ordens.Ordem lerOrdem(Object no) {
        if (no == null) {
            return null;
        }
        String tipo = Json.texto(Json.objeto(no, "Ordem").get("tipo"));
        if (tipo == null) {
            throw new JsonInvalidoException("Ordem: falta o campo tipo");
        }
        return switch (tipo) {
            case "DefinirPrioridade" -> lerOrdem_DefinirPrioridade(no);
            case "LiberarGate" -> lerOrdem_LiberarGate(no);
            case "SegurarGate" -> lerOrdem_SegurarGate(no);
            case "DecidirNc" -> lerOrdem_DecidirNc(no);
            case "Contratar" -> lerOrdem_Contratar(no);
            case "Treinar" -> lerOrdem_Treinar(no);
            case "FazerUmAUm" -> lerOrdem_FazerUmAUm(no);
            case "EscolherDecisaoConselho" -> lerOrdem_EscolherDecisaoConselho(no);
            case "MudarVelocidade" -> lerOrdem_MudarVelocidade(no);
            default -> throw new JsonInvalidoException("Ordem: tipo desconhecido " + tipo);
        };
    }

    public static void escreverOrdem(StringBuilder sb, br.marellar.engine.ordens.Ordem v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        if (v instanceof br.marellar.engine.ordens.Ordem.DefinirPrioridade x) {
            escreverOrdem_DefinirPrioridade(sb, x);
            return;
        }
        if (v instanceof br.marellar.engine.ordens.Ordem.LiberarGate x) {
            escreverOrdem_LiberarGate(sb, x);
            return;
        }
        if (v instanceof br.marellar.engine.ordens.Ordem.SegurarGate x) {
            escreverOrdem_SegurarGate(sb, x);
            return;
        }
        if (v instanceof br.marellar.engine.ordens.Ordem.DecidirNc x) {
            escreverOrdem_DecidirNc(sb, x);
            return;
        }
        if (v instanceof br.marellar.engine.ordens.Ordem.Contratar x) {
            escreverOrdem_Contratar(sb, x);
            return;
        }
        if (v instanceof br.marellar.engine.ordens.Ordem.Treinar x) {
            escreverOrdem_Treinar(sb, x);
            return;
        }
        if (v instanceof br.marellar.engine.ordens.Ordem.FazerUmAUm x) {
            escreverOrdem_FazerUmAUm(sb, x);
            return;
        }
        if (v instanceof br.marellar.engine.ordens.Ordem.EscolherDecisaoConselho x) {
            escreverOrdem_EscolherDecisaoConselho(sb, x);
            return;
        }
        if (v instanceof br.marellar.engine.ordens.Ordem.MudarVelocidade x) {
            escreverOrdem_MudarVelocidade(sb, x);
            return;
        }
        throw new IllegalArgumentException("Ordem sem escritor: " + v);
    }

    public static br.marellar.engine.dominio.DecisaoNc lerDecisaoNc(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.DecisaoNc.valueOf(s);
    }

    public static void escreverDecisaoNc(StringBuilder sb, br.marellar.engine.dominio.DecisaoNc v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.dominio.Treinamento lerTreinamento(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.Treinamento.valueOf(s);
    }

    public static void escreverTreinamento(StringBuilder sb, br.marellar.engine.dominio.Treinamento v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.dominio.Velocidade lerVelocidade(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.Velocidade.valueOf(s);
    }

    public static void escreverVelocidade(StringBuilder sb, br.marellar.engine.dominio.Velocidade v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.save.Save lerSave(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Save");
        return new br.marellar.engine.save.Save(
                Json.inteiro(m.get("versao")),
                Json.longo(m.get("semente")),
                Json.texto(m.get("inicio")),
                Json.longo(m.get("tick")),
                Json.lista(m.get("ordens"), o -> lerSave_OrdemNoTempo(o)),
                lerSave_Verificacao(m.get("verificacao")));
    }

    public static void escreverSave(StringBuilder sb, br.marellar.engine.save.Save v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"versao\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.versao());
        sb.append(",\"semente\":");
        ((Json.Escritor<Long>) Json::numero).escrever(sb, v.semente());
        sb.append(",\"inicio\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.inicio());
        sb.append(",\"tick\":");
        ((Json.Escritor<Long>) Json::numero).escrever(sb, v.tick());
        sb.append(",\"ordens\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.save.Save.OrdemNoTempo>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.save.Save.OrdemNoTempo>) CodecGerado::escreverSave_OrdemNoTempo)).escrever(sb, v.ordens());
        sb.append(",\"verificacao\":");
        ((Json.Escritor<br.marellar.engine.save.Save.Verificacao>) CodecGerado::escreverSave_Verificacao).escrever(sb, v.verificacao());
        sb.append("}");
    }

    public static br.marellar.engine.save.Save.OrdemNoTempo lerSave_OrdemNoTempo(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "OrdemNoTempo");
        return new br.marellar.engine.save.Save.OrdemNoTempo(
                Json.longo(m.get("tick")),
                lerOrdem(m.get("ordem")));
    }

    public static void escreverSave_OrdemNoTempo(StringBuilder sb, br.marellar.engine.save.Save.OrdemNoTempo v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"tick\":");
        ((Json.Escritor<Long>) Json::numero).escrever(sb, v.tick());
        sb.append(",\"ordem\":");
        ((Json.Escritor<br.marellar.engine.ordens.Ordem>) CodecGerado::escreverOrdem).escrever(sb, v.ordem());
        sb.append("}");
    }

    public static br.marellar.engine.save.Save.Verificacao lerSave_Verificacao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Verificacao");
        return new br.marellar.engine.save.Save.Verificacao(
                Json.texto(m.get("data")),
                Json.inteiro(m.get("hora")),
                Json.decimal(m.get("caixa")),
                Json.inteiro(m.get("eventos")),
                Json.texto(m.get("assinatura")));
    }

    public static void escreverSave_Verificacao(StringBuilder sb, br.marellar.engine.save.Save.Verificacao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"data\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.data());
        sb.append(",\"hora\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.hora());
        sb.append(",\"caixa\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.caixa());
        sb.append(",\"eventos\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.eventos());
        sb.append(",\"assinatura\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.assinatura());
        sb.append("}");
    }

    public static br.marellar.engine.estado.EstadoPartida lerEstadoPartida(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "EstadoPartida");
        return new br.marellar.engine.estado.EstadoPartida(
                Json.longo(m.get("semente")),
                Json.longo(m.get("tick")),
                Json.data(m.get("data")),
                Json.inteiro(m.get("hora")),
                Json.inteiro(m.get("mes")),
                Json.texto(m.get("mesReferencia")),
                Json.inteiro(m.get("diaUtilDoMes")),
                lerVelocidade(m.get("velocidade")),
                Json.logico(m.get("pausado")),
                lerMotivoPausa(m.get("motivoPausa")),
                Json.lista(m.get("barcos"), o -> lerEstadoPartida_BarcoVisao(o)),
                Json.lista(m.get("pessoas"), o -> lerEstadoPartida_PessoaVisao(o)),
                lerEstadoPartida_InspetorVisao(m.get("inspetor")),
                Json.texto(m.get("barcoEmInspecao")),
                Json.lista(m.get("filaInspecao"), o -> Json.texto(o)),
                lerEstadoPartida_QualidadeVisao(m.get("qualidade")),
                Json.lista(m.get("fornecedores"), o -> lerEstadoPartida_FornecedorVisao(o)),
                Json.mapa(m.get("estoque"), k -> k, o -> Json.inteiroOuNulo(o)),
                Json.lista(m.get("pedidosACaminho"), o -> lerEstadoPartida_PedidoVisao(o)),
                Json.lista(m.get("rncs"), o -> lerEstadoPartida_RncVisao(o)),
                lerEstadoPartida_SegurancaVisao(m.get("seguranca")),
                lerEstadoPartida_FinanceiroVisao(m.get("financeiro")),
                lerRelatorioConselho(m.get("conselho")),
                lerMotivoDerrota(m.get("derrota")),
                Json.decimal(m.get("horasOciosas")),
                Json.longo(m.get("ultimoEventoId")));
    }

    public static void escreverEstadoPartida(StringBuilder sb, br.marellar.engine.estado.EstadoPartida v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"semente\":");
        ((Json.Escritor<Long>) Json::numero).escrever(sb, v.semente());
        sb.append(",\"tick\":");
        ((Json.Escritor<Long>) Json::numero).escrever(sb, v.tick());
        sb.append(",\"data\":");
        ((Json.Escritor<java.time.LocalDate>) Json::data).escrever(sb, v.data());
        sb.append(",\"hora\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.hora());
        sb.append(",\"mes\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.mes());
        sb.append(",\"mesReferencia\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.mesReferencia());
        sb.append(",\"diaUtilDoMes\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diaUtilDoMes());
        sb.append(",\"velocidade\":");
        ((Json.Escritor<br.marellar.engine.dominio.Velocidade>) CodecGerado::escreverVelocidade).escrever(sb, v.velocidade());
        sb.append(",\"pausado\":");
        ((Json.Escritor<Boolean>) Json::numero).escrever(sb, v.pausado());
        sb.append(",\"motivoPausa\":");
        ((Json.Escritor<br.marellar.engine.dominio.MotivoPausa>) CodecGerado::escreverMotivoPausa).escrever(sb, v.motivoPausa());
        sb.append(",\"barcos\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.estado.EstadoPartida.BarcoVisao>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.estado.EstadoPartida.BarcoVisao>) CodecGerado::escreverEstadoPartida_BarcoVisao)).escrever(sb, v.barcos());
        sb.append(",\"pessoas\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.estado.EstadoPartida.PessoaVisao>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.estado.EstadoPartida.PessoaVisao>) CodecGerado::escreverEstadoPartida_PessoaVisao)).escrever(sb, v.pessoas());
        sb.append(",\"inspetor\":");
        ((Json.Escritor<br.marellar.engine.estado.EstadoPartida.InspetorVisao>) CodecGerado::escreverEstadoPartida_InspetorVisao).escrever(sb, v.inspetor());
        sb.append(",\"barcoEmInspecao\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.barcoEmInspecao());
        sb.append(",\"filaInspecao\":");
        ((Json.Escritor<java.util.List<java.lang.String>>) (s, x) -> Json.lista(s, x, (Json.Escritor<java.lang.String>) Json::texto)).escrever(sb, v.filaInspecao());
        sb.append(",\"qualidade\":");
        ((Json.Escritor<br.marellar.engine.estado.EstadoPartida.QualidadeVisao>) CodecGerado::escreverEstadoPartida_QualidadeVisao).escrever(sb, v.qualidade());
        sb.append(",\"fornecedores\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.estado.EstadoPartida.FornecedorVisao>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.estado.EstadoPartida.FornecedorVisao>) CodecGerado::escreverEstadoPartida_FornecedorVisao)).escrever(sb, v.fornecedores());
        sb.append(",\"estoque\":");
        ((Json.Escritor<java.util.Map<java.lang.String, java.lang.Integer>>) (s, x) -> Json.mapa(s, x, (Json.Escritor<java.lang.Integer>) Json::numero)).escrever(sb, v.estoque());
        sb.append(",\"pedidosACaminho\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.estado.EstadoPartida.PedidoVisao>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.estado.EstadoPartida.PedidoVisao>) CodecGerado::escreverEstadoPartida_PedidoVisao)).escrever(sb, v.pedidosACaminho());
        sb.append(",\"rncs\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.estado.EstadoPartida.RncVisao>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.estado.EstadoPartida.RncVisao>) CodecGerado::escreverEstadoPartida_RncVisao)).escrever(sb, v.rncs());
        sb.append(",\"seguranca\":");
        ((Json.Escritor<br.marellar.engine.estado.EstadoPartida.SegurancaVisao>) CodecGerado::escreverEstadoPartida_SegurancaVisao).escrever(sb, v.seguranca());
        sb.append(",\"financeiro\":");
        ((Json.Escritor<br.marellar.engine.estado.EstadoPartida.FinanceiroVisao>) CodecGerado::escreverEstadoPartida_FinanceiroVisao).escrever(sb, v.financeiro());
        sb.append(",\"conselho\":");
        ((Json.Escritor<br.marellar.engine.relatorio.RelatorioConselho>) CodecGerado::escreverRelatorioConselho).escrever(sb, v.conselho());
        sb.append(",\"derrota\":");
        ((Json.Escritor<br.marellar.engine.dominio.MotivoDerrota>) CodecGerado::escreverMotivoDerrota).escrever(sb, v.derrota());
        sb.append(",\"horasOciosas\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.horasOciosas());
        sb.append(",\"ultimoEventoId\":");
        ((Json.Escritor<Long>) Json::numero).escrever(sb, v.ultimoEventoId());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.MotivoPausa lerMotivoPausa(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.MotivoPausa.valueOf(s);
    }

    public static void escreverMotivoPausa(StringBuilder sb, br.marellar.engine.dominio.MotivoPausa v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.estado.EstadoPartida.BarcoVisao lerEstadoPartida_BarcoVisao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "BarcoVisao");
        return new br.marellar.engine.estado.EstadoPartida.BarcoVisao(
                Json.texto(m.get("id")),
                lerLinha(m.get("linha")),
                Json.texto(m.get("cliente")),
                lerCodigoGate(m.get("gateAtual")),
                Json.texto(m.get("nomeGate")),
                lerStatusBarco(m.get("status")),
                Json.logico(m.get("retido")),
                Json.inteiro(m.get("prioridade")),
                Json.decimal(m.get("hhTotalDoGate")),
                Json.decimal(m.get("hhRestantesNoGate")),
                Json.decimal(m.get("progressoGate")),
                Json.decimal(m.get("hhRetrabalhoPendente")),
                Json.decimal(m.get("hhQuebraPendente")),
                Json.decimal(m.get("hhProduzidas")),
                Json.decimal(m.get("hhRetrabalho")),
                Json.decimal(m.get("hhApontadas")),
                Json.longo(m.get("horasParadoMaterial")),
                Json.lista(m.get("gatesAprovados"), o -> lerCodigoGate(o)),
                Json.lista(m.get("pessoasAlocadas"), o -> Json.texto(o)),
                Json.lista(m.get("ncs"), o -> lerEstadoPartida_NcVisao(o)),
                Json.decimal(m.get("hhPrevistas")),
                Json.decimal(m.get("hhConcluidas")),
                Json.decimal(m.get("materialPrevisto")),
                Json.decimal(m.get("materialAplicado")),
                Json.decimal(m.get("precoVenda")),
                Json.decimal(m.get("valorRecebido")),
                Json.lista(m.get("cotas"), o -> lerEstadoPartida_CotaVisao(o)));
    }

    public static void escreverEstadoPartida_BarcoVisao(StringBuilder sb, br.marellar.engine.estado.EstadoPartida.BarcoVisao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"linha\":");
        ((Json.Escritor<br.marellar.engine.dominio.Linha>) CodecGerado::escreverLinha).escrever(sb, v.linha());
        sb.append(",\"cliente\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.cliente());
        sb.append(",\"gateAtual\":");
        ((Json.Escritor<br.marellar.engine.dominio.CodigoGate>) CodecGerado::escreverCodigoGate).escrever(sb, v.gateAtual());
        sb.append(",\"nomeGate\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.nomeGate());
        sb.append(",\"status\":");
        ((Json.Escritor<br.marellar.engine.dominio.StatusBarco>) CodecGerado::escreverStatusBarco).escrever(sb, v.status());
        sb.append(",\"retido\":");
        ((Json.Escritor<Boolean>) Json::numero).escrever(sb, v.retido());
        sb.append(",\"prioridade\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.prioridade());
        sb.append(",\"hhTotalDoGate\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhTotalDoGate());
        sb.append(",\"hhRestantesNoGate\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhRestantesNoGate());
        sb.append(",\"progressoGate\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.progressoGate());
        sb.append(",\"hhRetrabalhoPendente\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhRetrabalhoPendente());
        sb.append(",\"hhQuebraPendente\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhQuebraPendente());
        sb.append(",\"hhProduzidas\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhProduzidas());
        sb.append(",\"hhRetrabalho\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhRetrabalho());
        sb.append(",\"hhApontadas\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhApontadas());
        sb.append(",\"horasParadoMaterial\":");
        ((Json.Escritor<Long>) Json::numero).escrever(sb, v.horasParadoMaterial());
        sb.append(",\"gatesAprovados\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.dominio.CodigoGate>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.dominio.CodigoGate>) CodecGerado::escreverCodigoGate)).escrever(sb, v.gatesAprovados());
        sb.append(",\"pessoasAlocadas\":");
        ((Json.Escritor<java.util.List<java.lang.String>>) (s, x) -> Json.lista(s, x, (Json.Escritor<java.lang.String>) Json::texto)).escrever(sb, v.pessoasAlocadas());
        sb.append(",\"ncs\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.estado.EstadoPartida.NcVisao>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.estado.EstadoPartida.NcVisao>) CodecGerado::escreverEstadoPartida_NcVisao)).escrever(sb, v.ncs());
        sb.append(",\"hhPrevistas\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhPrevistas());
        sb.append(",\"hhConcluidas\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhConcluidas());
        sb.append(",\"materialPrevisto\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.materialPrevisto());
        sb.append(",\"materialAplicado\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.materialAplicado());
        sb.append(",\"precoVenda\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.precoVenda());
        sb.append(",\"valorRecebido\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.valorRecebido());
        sb.append(",\"cotas\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.estado.EstadoPartida.CotaVisao>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.estado.EstadoPartida.CotaVisao>) CodecGerado::escreverEstadoPartida_CotaVisao)).escrever(sb, v.cotas());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.StatusBarco lerStatusBarco(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.StatusBarco.valueOf(s);
    }

    public static void escreverStatusBarco(StringBuilder sb, br.marellar.engine.dominio.StatusBarco v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.estado.EstadoPartida.NcVisao lerEstadoPartida_NcVisao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "NcVisao");
        return new br.marellar.engine.estado.EstadoPartida.NcVisao(
                Json.texto(m.get("id")),
                lerCodigoGate(m.get("gate")),
                Json.texto(m.get("tipo")),
                Json.texto(m.get("descricao")),
                lerGravidade(m.get("gravidade")),
                lerOrigem(m.get("origem")),
                Json.texto(m.get("fornecedorId")),
                lerStatusNc(m.get("status")),
                lerDecisaoNc(m.get("decisao")),
                Json.decimal(m.get("hhRetrabalho")),
                Json.decimal(m.get("custoMaterial")));
    }

    public static void escreverEstadoPartida_NcVisao(StringBuilder sb, br.marellar.engine.estado.EstadoPartida.NcVisao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"gate\":");
        ((Json.Escritor<br.marellar.engine.dominio.CodigoGate>) CodecGerado::escreverCodigoGate).escrever(sb, v.gate());
        sb.append(",\"tipo\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.tipo());
        sb.append(",\"descricao\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.descricao());
        sb.append(",\"gravidade\":");
        ((Json.Escritor<br.marellar.engine.dominio.Gravidade>) CodecGerado::escreverGravidade).escrever(sb, v.gravidade());
        sb.append(",\"origem\":");
        ((Json.Escritor<br.marellar.engine.dominio.Origem>) CodecGerado::escreverOrigem).escrever(sb, v.origem());
        sb.append(",\"fornecedorId\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.fornecedorId());
        sb.append(",\"status\":");
        ((Json.Escritor<br.marellar.engine.dominio.StatusNc>) CodecGerado::escreverStatusNc).escrever(sb, v.status());
        sb.append(",\"decisao\":");
        ((Json.Escritor<br.marellar.engine.dominio.DecisaoNc>) CodecGerado::escreverDecisaoNc).escrever(sb, v.decisao());
        sb.append(",\"hhRetrabalho\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhRetrabalho());
        sb.append(",\"custoMaterial\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.custoMaterial());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.StatusNc lerStatusNc(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.StatusNc.valueOf(s);
    }

    public static void escreverStatusNc(StringBuilder sb, br.marellar.engine.dominio.StatusNc v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.estado.EstadoPartida.CotaVisao lerEstadoPartida_CotaVisao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "CotaVisao");
        return new br.marellar.engine.estado.EstadoPartida.CotaVisao(
                lerCodigoGate(m.get("gate")),
                Json.texto(m.get("descricao")),
                Json.decimal(m.get("percentual")),
                Json.decimal(m.get("valor")),
                lerEstadoPartida_SituacaoCota(m.get("situacao")),
                Json.longoOuNulo(m.get("diaRecebimento")));
    }

    public static void escreverEstadoPartida_CotaVisao(StringBuilder sb, br.marellar.engine.estado.EstadoPartida.CotaVisao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"gate\":");
        ((Json.Escritor<br.marellar.engine.dominio.CodigoGate>) CodecGerado::escreverCodigoGate).escrever(sb, v.gate());
        sb.append(",\"descricao\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.descricao());
        sb.append(",\"percentual\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.percentual());
        sb.append(",\"valor\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.valor());
        sb.append(",\"situacao\":");
        ((Json.Escritor<br.marellar.engine.estado.EstadoPartida.SituacaoCota>) CodecGerado::escreverEstadoPartida_SituacaoCota).escrever(sb, v.situacao());
        sb.append(",\"diaRecebimento\":");
        ((Json.Escritor<java.lang.Long>) Json::numero).escrever(sb, v.diaRecebimento());
        sb.append("}");
    }

    public static br.marellar.engine.estado.EstadoPartida.SituacaoCota lerEstadoPartida_SituacaoCota(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.estado.EstadoPartida.SituacaoCota.valueOf(s);
    }

    public static void escreverEstadoPartida_SituacaoCota(StringBuilder sb, br.marellar.engine.estado.EstadoPartida.SituacaoCota v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.estado.EstadoPartida.PessoaVisao lerEstadoPartida_PessoaVisao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "PessoaVisao");
        return new br.marellar.engine.estado.EstadoPartida.PessoaVisao(
                Json.texto(m.get("id")),
                Json.texto(m.get("nome")),
                lerFuncao(m.get("funcao")),
                Json.inteiro(m.get("nivel")),
                Json.inteiro(m.get("moral")),
                lerSituacaoPessoa(m.get("situacao")),
                Json.inteiro(m.get("diasRestantesSituacao")),
                lerTreinamento(m.get("treinamento")),
                Json.logico(m.get("treinoSegurancaEmDia")),
                Json.logico(m.get("contratadaNaPartida")),
                Json.logico(m.get("podeFazerUmAUm")),
                Json.texto(m.get("alocadoEm")));
    }

    public static void escreverEstadoPartida_PessoaVisao(StringBuilder sb, br.marellar.engine.estado.EstadoPartida.PessoaVisao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"nome\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.nome());
        sb.append(",\"funcao\":");
        ((Json.Escritor<br.marellar.engine.dominio.Funcao>) CodecGerado::escreverFuncao).escrever(sb, v.funcao());
        sb.append(",\"nivel\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.nivel());
        sb.append(",\"moral\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.moral());
        sb.append(",\"situacao\":");
        ((Json.Escritor<br.marellar.engine.dominio.SituacaoPessoa>) CodecGerado::escreverSituacaoPessoa).escrever(sb, v.situacao());
        sb.append(",\"diasRestantesSituacao\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasRestantesSituacao());
        sb.append(",\"treinamento\":");
        ((Json.Escritor<br.marellar.engine.dominio.Treinamento>) CodecGerado::escreverTreinamento).escrever(sb, v.treinamento());
        sb.append(",\"treinoSegurancaEmDia\":");
        ((Json.Escritor<Boolean>) Json::numero).escrever(sb, v.treinoSegurancaEmDia());
        sb.append(",\"contratadaNaPartida\":");
        ((Json.Escritor<Boolean>) Json::numero).escrever(sb, v.contratadaNaPartida());
        sb.append(",\"podeFazerUmAUm\":");
        ((Json.Escritor<Boolean>) Json::numero).escrever(sb, v.podeFazerUmAUm());
        sb.append(",\"alocadoEm\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.alocadoEm());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.SituacaoPessoa lerSituacaoPessoa(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.SituacaoPessoa.valueOf(s);
    }

    public static void escreverSituacaoPessoa(StringBuilder sb, br.marellar.engine.dominio.SituacaoPessoa v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.estado.EstadoPartida.InspetorVisao lerEstadoPartida_InspetorVisao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "InspetorVisao");
        return new br.marellar.engine.estado.EstadoPartida.InspetorVisao(
                Json.texto(m.get("id")),
                Json.texto(m.get("nome")),
                Json.inteiro(m.get("moral")),
                Json.inteiro(m.get("rigor")),
                Json.inteiro(m.get("velocidade")),
                Json.inteiro(m.get("paciencia")));
    }

    public static void escreverEstadoPartida_InspetorVisao(StringBuilder sb, br.marellar.engine.estado.EstadoPartida.InspetorVisao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"nome\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.nome());
        sb.append(",\"moral\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.moral());
        sb.append(",\"rigor\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.rigor());
        sb.append(",\"velocidade\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.velocidade());
        sb.append(",\"paciencia\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.paciencia());
        sb.append("}");
    }

    public static br.marellar.engine.estado.EstadoPartida.QualidadeVisao lerEstadoPartida_QualidadeVisao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "QualidadeVisao");
        return new br.marellar.engine.estado.EstadoPartida.QualidadeVisao(
                Json.inteiro(m.get("ncsDetectadas")),
                Json.inteiro(m.get("ncsAbertas")),
                Json.inteiro(m.get("concessoes")),
                Json.inteiro(m.get("rncs")),
                Json.decimal(m.get("hhRetrabalho")),
                Json.decimal(m.get("custoMaterialRetrabalho")));
    }

    public static void escreverEstadoPartida_QualidadeVisao(StringBuilder sb, br.marellar.engine.estado.EstadoPartida.QualidadeVisao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"ncsDetectadas\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.ncsDetectadas());
        sb.append(",\"ncsAbertas\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.ncsAbertas());
        sb.append(",\"concessoes\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.concessoes());
        sb.append(",\"rncs\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.rncs());
        sb.append(",\"hhRetrabalho\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.hhRetrabalho());
        sb.append(",\"custoMaterialRetrabalho\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.custoMaterialRetrabalho());
        sb.append("}");
    }

    public static br.marellar.engine.estado.EstadoPartida.FornecedorVisao lerEstadoPartida_FornecedorVisao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "FornecedorVisao");
        return new br.marellar.engine.estado.EstadoPartida.FornecedorVisao(
                Json.texto(m.get("id")),
                Json.texto(m.get("nome")),
                Json.lista(m.get("itens"), o -> Json.texto(o)),
                Json.texto(m.get("nota")),
                Json.inteiro(m.get("pontos")),
                Json.longo(m.get("ppm")),
                Json.inteiro(m.get("pedidosEntregues")),
                Json.inteiro(m.get("pedidosNoPrazo")),
                Json.inteiro(m.get("oitoDsRespondidos")),
                Json.inteiro(m.get("oitoDsNoPrazo")),
                Json.decimal(m.get("creditoChargeback")),
                Json.decimal(m.get("totalChargeback")),
                Json.inteiro(m.get("rncsAbertas")));
    }

    public static void escreverEstadoPartida_FornecedorVisao(StringBuilder sb, br.marellar.engine.estado.EstadoPartida.FornecedorVisao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"nome\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.nome());
        sb.append(",\"itens\":");
        ((Json.Escritor<java.util.List<java.lang.String>>) (s, x) -> Json.lista(s, x, (Json.Escritor<java.lang.String>) Json::texto)).escrever(sb, v.itens());
        sb.append(",\"nota\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.nota());
        sb.append(",\"pontos\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.pontos());
        sb.append(",\"ppm\":");
        ((Json.Escritor<Long>) Json::numero).escrever(sb, v.ppm());
        sb.append(",\"pedidosEntregues\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.pedidosEntregues());
        sb.append(",\"pedidosNoPrazo\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.pedidosNoPrazo());
        sb.append(",\"oitoDsRespondidos\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.oitoDsRespondidos());
        sb.append(",\"oitoDsNoPrazo\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.oitoDsNoPrazo());
        sb.append(",\"creditoChargeback\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.creditoChargeback());
        sb.append(",\"totalChargeback\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.totalChargeback());
        sb.append(",\"rncsAbertas\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.rncsAbertas());
        sb.append("}");
    }

    public static br.marellar.engine.estado.EstadoPartida.PedidoVisao lerEstadoPartida_PedidoVisao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "PedidoVisao");
        return new br.marellar.engine.estado.EstadoPartida.PedidoVisao(
                Json.texto(m.get("id")),
                Json.texto(m.get("fornecedor")),
                Json.texto(m.get("item")),
                Json.inteiro(m.get("kits")),
                Json.data(m.get("dataPrevista")),
                Json.decimal(m.get("valorPago")));
    }

    public static void escreverEstadoPartida_PedidoVisao(StringBuilder sb, br.marellar.engine.estado.EstadoPartida.PedidoVisao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"fornecedor\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.fornecedor());
        sb.append(",\"item\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.item());
        sb.append(",\"kits\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.kits());
        sb.append(",\"dataPrevista\":");
        ((Json.Escritor<java.time.LocalDate>) Json::data).escrever(sb, v.dataPrevista());
        sb.append(",\"valorPago\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.valorPago());
        sb.append("}");
    }

    public static br.marellar.engine.estado.EstadoPartida.RncVisao lerEstadoPartida_RncVisao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "RncVisao");
        return new br.marellar.engine.estado.EstadoPartida.RncVisao(
                Json.texto(m.get("id")),
                Json.texto(m.get("ncId")),
                Json.texto(m.get("barcoId")),
                Json.texto(m.get("fornecedorId")),
                lerGravidade(m.get("gravidade")),
                Json.decimal(m.get("custoDocumentado")),
                Json.decimal(m.get("chargeback")),
                Json.data(m.get("prazo8d")),
                lerStatusRnc(m.get("status")),
                Json.decimal(m.get("chargebackHorasParadas")));
    }

    public static void escreverEstadoPartida_RncVisao(StringBuilder sb, br.marellar.engine.estado.EstadoPartida.RncVisao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"ncId\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.ncId());
        sb.append(",\"barcoId\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.barcoId());
        sb.append(",\"fornecedorId\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.fornecedorId());
        sb.append(",\"gravidade\":");
        ((Json.Escritor<br.marellar.engine.dominio.Gravidade>) CodecGerado::escreverGravidade).escrever(sb, v.gravidade());
        sb.append(",\"custoDocumentado\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.custoDocumentado());
        sb.append(",\"chargeback\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.chargeback());
        sb.append(",\"prazo8d\":");
        ((Json.Escritor<java.time.LocalDate>) Json::data).escrever(sb, v.prazo8d());
        sb.append(",\"status\":");
        ((Json.Escritor<br.marellar.engine.dominio.StatusRnc>) CodecGerado::escreverStatusRnc).escrever(sb, v.status());
        sb.append(",\"chargebackHorasParadas\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.chargebackHorasParadas());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.StatusRnc lerStatusRnc(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.StatusRnc.valueOf(s);
    }

    public static void escreverStatusRnc(StringBuilder sb, br.marellar.engine.dominio.StatusRnc v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.estado.EstadoPartida.SegurancaVisao lerEstadoPartida_SegurancaVisao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "SegurancaVisao");
        return new br.marellar.engine.estado.EstadoPartida.SegurancaVisao(
                Json.inteiro(m.get("acidentes")),
                Json.inteiro(m.get("diasPerdidos")),
                Json.decimal(m.get("horasTrabalhadas")),
                Json.decimal(m.get("taxaFrequencia")),
                Json.decimal(m.get("taxaGravidade")),
                Json.inteiro(m.get("demissoes")),
                Json.decimal(m.get("horasLiderUmAUm")));
    }

    public static void escreverEstadoPartida_SegurancaVisao(StringBuilder sb, br.marellar.engine.estado.EstadoPartida.SegurancaVisao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"acidentes\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.acidentes());
        sb.append(",\"diasPerdidos\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.diasPerdidos());
        sb.append(",\"horasTrabalhadas\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.horasTrabalhadas());
        sb.append(",\"taxaFrequencia\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.taxaFrequencia());
        sb.append(",\"taxaGravidade\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.taxaGravidade());
        sb.append(",\"demissoes\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.demissoes());
        sb.append(",\"horasLiderUmAUm\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.horasLiderUmAUm());
        sb.append("}");
    }

    public static br.marellar.engine.estado.EstadoPartida.FinanceiroVisao lerEstadoPartida_FinanceiroVisao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "FinanceiroVisao");
        return new br.marellar.engine.estado.EstadoPartida.FinanceiroVisao(
                Json.decimal(m.get("caixa")),
                Json.decimal(m.get("cotasAReceber")),
                Json.decimal(m.get("valorEmpresa")),
                Json.inteiro(m.get("pacienciaConselho")),
                Json.inteiro(m.get("pacienciaMaxima")),
                Json.inteiro(m.get("mesesCaixaNegativo")),
                Json.decimal(m.get("receitaDoMes")),
                Json.lista(m.get("ultimosLancamentos"), o -> lerLancamento(o)));
    }

    public static void escreverEstadoPartida_FinanceiroVisao(StringBuilder sb, br.marellar.engine.estado.EstadoPartida.FinanceiroVisao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"caixa\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.caixa());
        sb.append(",\"cotasAReceber\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.cotasAReceber());
        sb.append(",\"valorEmpresa\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.valorEmpresa());
        sb.append(",\"pacienciaConselho\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.pacienciaConselho());
        sb.append(",\"pacienciaMaxima\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.pacienciaMaxima());
        sb.append(",\"mesesCaixaNegativo\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.mesesCaixaNegativo());
        sb.append(",\"receitaDoMes\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.receitaDoMes());
        sb.append(",\"ultimosLancamentos\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.dominio.Lancamento>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.dominio.Lancamento>) CodecGerado::escreverLancamento)).escrever(sb, v.ultimosLancamentos());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.Lancamento lerLancamento(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Lancamento");
        return new br.marellar.engine.dominio.Lancamento(
                Json.data(m.get("data")),
                lerTipoLancamento(m.get("tipo")),
                Json.decimal(m.get("valor")),
                Json.texto(m.get("descricao")));
    }

    public static void escreverLancamento(StringBuilder sb, br.marellar.engine.dominio.Lancamento v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"data\":");
        ((Json.Escritor<java.time.LocalDate>) Json::data).escrever(sb, v.data());
        sb.append(",\"tipo\":");
        ((Json.Escritor<br.marellar.engine.dominio.TipoLancamento>) CodecGerado::escreverTipoLancamento).escrever(sb, v.tipo());
        sb.append(",\"valor\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.valor());
        sb.append(",\"descricao\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.descricao());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.TipoLancamento lerTipoLancamento(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.TipoLancamento.valueOf(s);
    }

    public static void escreverTipoLancamento(StringBuilder sb, br.marellar.engine.dominio.TipoLancamento v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.relatorio.RelatorioConselho lerRelatorioConselho(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "RelatorioConselho");
        return new br.marellar.engine.relatorio.RelatorioConselho(
                Json.inteiro(m.get("mes")),
                Json.texto(m.get("mesReferencia")),
                Json.lista(m.get("metas"), o -> lerRelatorioConselho_Meta(o)),
                lerDre(m.get("dre")),
                Json.decimal(m.get("caixa")),
                Json.decimal(m.get("valorEmpresa")),
                Json.decimal(m.get("descontoValuation")),
                Json.inteiro(m.get("barcosEntregues")),
                Json.inteiro(m.get("ncsDetectadas")),
                Json.inteiro(m.get("rncs")),
                Json.inteiro(m.get("concessoes")),
                Json.inteiro(m.get("acidentes")),
                Json.decimal(m.get("taxaFrequencia")),
                Json.decimal(m.get("taxaGravidade")),
                Json.inteiro(m.get("pacienciaAntes")),
                Json.inteiro(m.get("pacienciaDepois")),
                Json.logico(m.get("alertaCaixaNegativo")),
                Json.lista(m.get("falas"), o -> lerRelatorioConselho_Fala(o)),
                Json.lista(m.get("opcoes"), o -> lerRelatorioConselho_Opcao(o)),
                Json.texto(m.get("decisaoEscolhida")),
                lerMotivoDerrota(m.get("derrota")));
    }

    public static void escreverRelatorioConselho(StringBuilder sb, br.marellar.engine.relatorio.RelatorioConselho v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"mes\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.mes());
        sb.append(",\"mesReferencia\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.mesReferencia());
        sb.append(",\"metas\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.relatorio.RelatorioConselho.Meta>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.relatorio.RelatorioConselho.Meta>) CodecGerado::escreverRelatorioConselho_Meta)).escrever(sb, v.metas());
        sb.append(",\"dre\":");
        ((Json.Escritor<br.marellar.engine.relatorio.Dre>) CodecGerado::escreverDre).escrever(sb, v.dre());
        sb.append(",\"caixa\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.caixa());
        sb.append(",\"valorEmpresa\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.valorEmpresa());
        sb.append(",\"descontoValuation\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.descontoValuation());
        sb.append(",\"barcosEntregues\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.barcosEntregues());
        sb.append(",\"ncsDetectadas\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.ncsDetectadas());
        sb.append(",\"rncs\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.rncs());
        sb.append(",\"concessoes\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.concessoes());
        sb.append(",\"acidentes\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.acidentes());
        sb.append(",\"taxaFrequencia\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.taxaFrequencia());
        sb.append(",\"taxaGravidade\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.taxaGravidade());
        sb.append(",\"pacienciaAntes\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.pacienciaAntes());
        sb.append(",\"pacienciaDepois\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.pacienciaDepois());
        sb.append(",\"alertaCaixaNegativo\":");
        ((Json.Escritor<Boolean>) Json::numero).escrever(sb, v.alertaCaixaNegativo());
        sb.append(",\"falas\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.relatorio.RelatorioConselho.Fala>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.relatorio.RelatorioConselho.Fala>) CodecGerado::escreverRelatorioConselho_Fala)).escrever(sb, v.falas());
        sb.append(",\"opcoes\":");
        ((Json.Escritor<java.util.List<br.marellar.engine.relatorio.RelatorioConselho.Opcao>>) (s, x) -> Json.lista(s, x, (Json.Escritor<br.marellar.engine.relatorio.RelatorioConselho.Opcao>) CodecGerado::escreverRelatorioConselho_Opcao)).escrever(sb, v.opcoes());
        sb.append(",\"decisaoEscolhida\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.decisaoEscolhida());
        sb.append(",\"derrota\":");
        ((Json.Escritor<br.marellar.engine.dominio.MotivoDerrota>) CodecGerado::escreverMotivoDerrota).escrever(sb, v.derrota());
        sb.append("}");
    }

    public static br.marellar.engine.relatorio.RelatorioConselho.Meta lerRelatorioConselho_Meta(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Meta");
        return new br.marellar.engine.relatorio.RelatorioConselho.Meta(
                Json.texto(m.get("id")),
                Json.texto(m.get("descricao")),
                Json.logico(m.get("cumprida")),
                Json.texto(m.get("resultado")));
    }

    public static void escreverRelatorioConselho_Meta(StringBuilder sb, br.marellar.engine.relatorio.RelatorioConselho.Meta v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"descricao\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.descricao());
        sb.append(",\"cumprida\":");
        ((Json.Escritor<Boolean>) Json::numero).escrever(sb, v.cumprida());
        sb.append(",\"resultado\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.resultado());
        sb.append("}");
    }

    public static br.marellar.engine.relatorio.Dre lerDre(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Dre");
        return new br.marellar.engine.relatorio.Dre(
                Json.decimal(m.get("receita")),
                Json.decimal(m.get("custoMaterial")),
                Json.decimal(m.get("consumiveis")),
                Json.decimal(m.get("materialRetrabalho")),
                Json.decimal(m.get("pessoal")),
                Json.decimal(m.get("despesasFixas")),
                Json.decimal(m.get("chargebackRecuperado")),
                Json.decimal(m.get("ebitda")));
    }

    public static void escreverDre(StringBuilder sb, br.marellar.engine.relatorio.Dre v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"receita\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.receita());
        sb.append(",\"custoMaterial\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.custoMaterial());
        sb.append(",\"consumiveis\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.consumiveis());
        sb.append(",\"materialRetrabalho\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.materialRetrabalho());
        sb.append(",\"pessoal\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.pessoal());
        sb.append(",\"despesasFixas\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.despesasFixas());
        sb.append(",\"chargebackRecuperado\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.chargebackRecuperado());
        sb.append(",\"ebitda\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.ebitda());
        sb.append("}");
    }

    public static br.marellar.engine.relatorio.RelatorioConselho.Fala lerRelatorioConselho_Fala(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Fala");
        return new br.marellar.engine.relatorio.RelatorioConselho.Fala(
                Json.texto(m.get("conselheiro")),
                Json.texto(m.get("papel")),
                Json.texto(m.get("texto")),
                Json.logico(m.get("seria")));
    }

    public static void escreverRelatorioConselho_Fala(StringBuilder sb, br.marellar.engine.relatorio.RelatorioConselho.Fala v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"conselheiro\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.conselheiro());
        sb.append(",\"papel\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.papel());
        sb.append(",\"texto\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.texto());
        sb.append(",\"seria\":");
        ((Json.Escritor<Boolean>) Json::numero).escrever(sb, v.seria());
        sb.append("}");
    }

    public static br.marellar.engine.relatorio.RelatorioConselho.Opcao lerRelatorioConselho_Opcao(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Opcao");
        return new br.marellar.engine.relatorio.RelatorioConselho.Opcao(
                Json.texto(m.get("id")),
                Json.texto(m.get("titulo")),
                Json.texto(m.get("descricao")),
                Json.decimal(m.get("custo")));
    }

    public static void escreverRelatorioConselho_Opcao(StringBuilder sb, br.marellar.engine.relatorio.RelatorioConselho.Opcao v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"titulo\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.titulo());
        sb.append(",\"descricao\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.descricao());
        sb.append(",\"custo\":");
        ((Json.Escritor<java.math.BigDecimal>) Json::decimal).escrever(sb, v.custo());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.MotivoDerrota lerMotivoDerrota(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.MotivoDerrota.valueOf(s);
    }

    public static void escreverMotivoDerrota(StringBuilder sb, br.marellar.engine.dominio.MotivoDerrota v) {
        Json.nome(sb, v);
    }

    public static br.marellar.engine.ResultadoAvanco lerResultadoAvanco(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "ResultadoAvanco");
        return new br.marellar.engine.ResultadoAvanco(
                Json.inteiro(m.get("ticksExecutados")),
                Json.logico(m.get("pausado")),
                lerMotivoPausa(m.get("motivoPausa")));
    }

    public static void escreverResultadoAvanco(StringBuilder sb, br.marellar.engine.ResultadoAvanco v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"ticksExecutados\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.ticksExecutados());
        sb.append(",\"pausado\":");
        ((Json.Escritor<Boolean>) Json::numero).escrever(sb, v.pausado());
        sb.append(",\"motivoPausa\":");
        ((Json.Escritor<br.marellar.engine.dominio.MotivoPausa>) CodecGerado::escreverMotivoPausa).escrever(sb, v.motivoPausa());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.Evento lerEvento(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Evento");
        return new br.marellar.engine.dominio.Evento(
                Json.longo(m.get("id")),
                Json.longo(m.get("tick")),
                Json.data(m.get("data")),
                Json.inteiro(m.get("hora")),
                lerTipoEvento(m.get("tipo")),
                Json.texto(m.get("barcoId")),
                Json.texto(m.get("mensagem")));
    }

    public static void escreverEvento(StringBuilder sb, br.marellar.engine.dominio.Evento v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<Long>) Json::numero).escrever(sb, v.id());
        sb.append(",\"tick\":");
        ((Json.Escritor<Long>) Json::numero).escrever(sb, v.tick());
        sb.append(",\"data\":");
        ((Json.Escritor<java.time.LocalDate>) Json::data).escrever(sb, v.data());
        sb.append(",\"hora\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.hora());
        sb.append(",\"tipo\":");
        ((Json.Escritor<br.marellar.engine.dominio.TipoEvento>) CodecGerado::escreverTipoEvento).escrever(sb, v.tipo());
        sb.append(",\"barcoId\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.barcoId());
        sb.append(",\"mensagem\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.mensagem());
        sb.append("}");
    }

    public static br.marellar.engine.dominio.TipoEvento lerTipoEvento(Object no) {
        String s = Json.texto(no);
        return s == null ? null : br.marellar.engine.dominio.TipoEvento.valueOf(s);
    }

    public static void escreverTipoEvento(StringBuilder sb, br.marellar.engine.dominio.TipoEvento v) {
        Json.nome(sb, v);
    }

    public static br.marellar.web.Respostas.CriarPartida lerRespostas_CriarPartida(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "CriarPartida");
        return new br.marellar.web.Respostas.CriarPartida(
                Json.texto(m.get("id")));
    }

    public static void escreverRespostas_CriarPartida(StringBuilder sb, br.marellar.web.Respostas.CriarPartida v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append("}");
    }

    public static br.marellar.web.Respostas.Avancar lerRespostas_Avancar(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Avancar");
        return new br.marellar.web.Respostas.Avancar(
                lerResultadoAvanco(m.get("resultado")),
                lerEstadoPartida(m.get("estado")));
    }

    public static void escreverRespostas_Avancar(StringBuilder sb, br.marellar.web.Respostas.Avancar v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"resultado\":");
        ((Json.Escritor<br.marellar.engine.ResultadoAvanco>) CodecGerado::escreverResultadoAvanco).escrever(sb, v.resultado());
        sb.append(",\"estado\":");
        ((Json.Escritor<br.marellar.engine.estado.EstadoPartida>) CodecGerado::escreverEstadoPartida).escrever(sb, v.estado());
        sb.append("}");
    }

    public static br.marellar.web.Respostas.ResumoSave lerRespostas_ResumoSave(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "ResumoSave");
        return new br.marellar.web.Respostas.ResumoSave(
                Json.texto(m.get("id")),
                Json.longo(m.get("semente")),
                Json.longo(m.get("tick")),
                Json.texto(m.get("data")),
                Json.inteiro(m.get("hora")),
                Json.texto(m.get("salvoEm")));
    }

    public static void escreverRespostas_ResumoSave(StringBuilder sb, br.marellar.web.Respostas.ResumoSave v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"id\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.id());
        sb.append(",\"semente\":");
        ((Json.Escritor<Long>) Json::numero).escrever(sb, v.semente());
        sb.append(",\"tick\":");
        ((Json.Escritor<Long>) Json::numero).escrever(sb, v.tick());
        sb.append(",\"data\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.data());
        sb.append(",\"hora\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.hora());
        sb.append(",\"salvoEm\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.salvoEm());
        sb.append("}");
    }

    public static br.marellar.engine.ordens.Ordem.DefinirPrioridade lerOrdem_DefinirPrioridade(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "DefinirPrioridade");
        return new br.marellar.engine.ordens.Ordem.DefinirPrioridade(
                Json.texto(m.get("barcoId")),
                Json.inteiro(m.get("posicao")));
    }

    public static void escreverOrdem_DefinirPrioridade(StringBuilder sb, br.marellar.engine.ordens.Ordem.DefinirPrioridade v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"tipo\":\"DefinirPrioridade\"");
        sb.append(",\"barcoId\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.barcoId());
        sb.append(",\"posicao\":");
        ((Json.Escritor<Integer>) Json::numero).escrever(sb, v.posicao());
        sb.append("}");
    }

    public static br.marellar.engine.ordens.Ordem.LiberarGate lerOrdem_LiberarGate(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "LiberarGate");
        return new br.marellar.engine.ordens.Ordem.LiberarGate(
                Json.texto(m.get("barcoId")));
    }

    public static void escreverOrdem_LiberarGate(StringBuilder sb, br.marellar.engine.ordens.Ordem.LiberarGate v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"tipo\":\"LiberarGate\"");
        sb.append(",\"barcoId\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.barcoId());
        sb.append("}");
    }

    public static br.marellar.engine.ordens.Ordem.SegurarGate lerOrdem_SegurarGate(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "SegurarGate");
        return new br.marellar.engine.ordens.Ordem.SegurarGate(
                Json.texto(m.get("barcoId")));
    }

    public static void escreverOrdem_SegurarGate(StringBuilder sb, br.marellar.engine.ordens.Ordem.SegurarGate v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"tipo\":\"SegurarGate\"");
        sb.append(",\"barcoId\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.barcoId());
        sb.append("}");
    }

    public static br.marellar.engine.ordens.Ordem.DecidirNc lerOrdem_DecidirNc(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "DecidirNc");
        return new br.marellar.engine.ordens.Ordem.DecidirNc(
                Json.texto(m.get("ncId")),
                lerDecisaoNc(m.get("decisao")));
    }

    public static void escreverOrdem_DecidirNc(StringBuilder sb, br.marellar.engine.ordens.Ordem.DecidirNc v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"tipo\":\"DecidirNc\"");
        sb.append(",\"ncId\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.ncId());
        sb.append(",\"decisao\":");
        ((Json.Escritor<br.marellar.engine.dominio.DecisaoNc>) CodecGerado::escreverDecisaoNc).escrever(sb, v.decisao());
        sb.append("}");
    }

    public static br.marellar.engine.ordens.Ordem.Contratar lerOrdem_Contratar(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Contratar");
        return new br.marellar.engine.ordens.Ordem.Contratar(
                lerFuncao(m.get("funcao")));
    }

    public static void escreverOrdem_Contratar(StringBuilder sb, br.marellar.engine.ordens.Ordem.Contratar v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"tipo\":\"Contratar\"");
        sb.append(",\"funcao\":");
        ((Json.Escritor<br.marellar.engine.dominio.Funcao>) CodecGerado::escreverFuncao).escrever(sb, v.funcao());
        sb.append("}");
    }

    public static br.marellar.engine.ordens.Ordem.Treinar lerOrdem_Treinar(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "Treinar");
        return new br.marellar.engine.ordens.Ordem.Treinar(
                Json.texto(m.get("pessoaId")),
                lerTreinamento(m.get("treinamento")));
    }

    public static void escreverOrdem_Treinar(StringBuilder sb, br.marellar.engine.ordens.Ordem.Treinar v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"tipo\":\"Treinar\"");
        sb.append(",\"pessoaId\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.pessoaId());
        sb.append(",\"treinamento\":");
        ((Json.Escritor<br.marellar.engine.dominio.Treinamento>) CodecGerado::escreverTreinamento).escrever(sb, v.treinamento());
        sb.append("}");
    }

    public static br.marellar.engine.ordens.Ordem.FazerUmAUm lerOrdem_FazerUmAUm(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "FazerUmAUm");
        return new br.marellar.engine.ordens.Ordem.FazerUmAUm(
                Json.texto(m.get("pessoaId")));
    }

    public static void escreverOrdem_FazerUmAUm(StringBuilder sb, br.marellar.engine.ordens.Ordem.FazerUmAUm v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"tipo\":\"FazerUmAUm\"");
        sb.append(",\"pessoaId\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.pessoaId());
        sb.append("}");
    }

    public static br.marellar.engine.ordens.Ordem.EscolherDecisaoConselho lerOrdem_EscolherDecisaoConselho(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "EscolherDecisaoConselho");
        return new br.marellar.engine.ordens.Ordem.EscolherDecisaoConselho(
                Json.texto(m.get("opcaoId")));
    }

    public static void escreverOrdem_EscolherDecisaoConselho(StringBuilder sb, br.marellar.engine.ordens.Ordem.EscolherDecisaoConselho v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"tipo\":\"EscolherDecisaoConselho\"");
        sb.append(",\"opcaoId\":");
        ((Json.Escritor<java.lang.String>) Json::texto).escrever(sb, v.opcaoId());
        sb.append("}");
    }

    public static br.marellar.engine.ordens.Ordem.MudarVelocidade lerOrdem_MudarVelocidade(Object no) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = Json.objeto(no, "MudarVelocidade");
        return new br.marellar.engine.ordens.Ordem.MudarVelocidade(
                lerVelocidade(m.get("velocidade")));
    }

    public static void escreverOrdem_MudarVelocidade(StringBuilder sb, br.marellar.engine.ordens.Ordem.MudarVelocidade v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        sb.append("{\"tipo\":\"MudarVelocidade\"");
        sb.append(",\"velocidade\":");
        ((Json.Escritor<br.marellar.engine.dominio.Velocidade>) CodecGerado::escreverVelocidade).escrever(sb, v.velocidade());
        sb.append("}");
    }
}
