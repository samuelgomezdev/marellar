package br.marellar.web.json;

import br.marellar.engine.ResultadoAvanco;
import br.marellar.engine.dados.CatalogoNc;
import br.marellar.engine.dados.ParametrosEventos;
import br.marellar.engine.dados.ParametrosFinanceiro;
import br.marellar.engine.dados.ParametrosFornecedores;
import br.marellar.engine.dados.ParametrosLinha;
import br.marellar.engine.dados.ParametrosPessoas;
import br.marellar.engine.dominio.Evento;
import br.marellar.engine.estado.EstadoPartida;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.relatorio.RelatorioConselho;
import br.marellar.engine.save.Save;
import br.marellar.web.Respostas;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gera {@code CodecGerado.java}: um leitor e um escritor de JSON para cada record, enum e ordem
 * usados pela API, sem reflexao (o navegador nao tem). Roda na JVM, olhando os records por reflexao.
 * <p>
 * Para regenerar depois de mudar um record do motor:
 * {@code mvnw -pl marellar-web test -Dtest=CodecEmDiaTest -Dmarellar.gerarCodec=true}
 */
public final class GeradorCodec {

    static final Path ARQUIVO = Path.of("src/main/java/br/marellar/web/json/CodecGerado.java");

    /** Tipos de entrada (dados e ordens) e de saida (respostas da API). */
    static final List<Class<?>> RAIZES = List.of(
            ParametrosLinha.class, CatalogoNc.class, ParametrosPessoas.class, ParametrosFornecedores.class,
            ParametrosEventos.class, ParametrosFinanceiro.class,
            Ordem.class, Save.class,
            EstadoPartida.class, EstadoPartida.BarcoVisao.class, ResultadoAvanco.class, Evento.class,
            RelatorioConselho.class,
            Respostas.CriarPartida.class, Respostas.Avancar.class, Respostas.ResumoSave.class);

    private final Set<Class<?>> tipos = new LinkedHashSet<>();

    public static void main(String[] args) throws Exception {
        Files.writeString(ARQUIVO, gerar(), StandardCharsets.UTF_8);
    }

    public static String gerar() {
        GeradorCodec g = new GeradorCodec();
        RAIZES.forEach(g::visitar);
        return g.codigo();
    }

    private void visitar(Type t) {
        if (t instanceof ParameterizedType p) {
            for (Type a : p.getActualTypeArguments()) {
                visitar(a);
            }
            return;
        }
        Class<?> c = (Class<?>) t;
        if (!c.getName().startsWith("br.marellar.") || !tipos.add(c)) {
            return;
        }
        if (c.isRecord()) {
            for (RecordComponent rc : c.getRecordComponents()) {
                visitar(rc.getGenericType());
            }
        } else if (c.isSealed()) {
            for (Class<?> sub : c.getPermittedSubclasses()) {
                visitar(sub);
            }
        } else if (!c.isEnum()) {
            throw new IllegalStateException("Tipo sem conversor: " + c.getName());
        }
    }

    private String codigo() {
        StringBuilder sb = new StringBuilder();
        sb.append("""
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
                """);
        for (Class<?> c : tipos) {
            if (c.isEnum()) {
                enumeracao(sb, c);
            } else if (c.isSealed()) {
                selado(sb, c);
            } else if (!(c.isRecord() && c.getEnclosingClass() != null && c.getEnclosingClass().isSealed())) {
                registro(sb, c, null);
            }
        }
        for (Class<?> c : tipos) {
            if (c.isRecord() && c.getEnclosingClass() != null && c.getEnclosingClass().isSealed()) {
                registro(sb, c, c.getSimpleName());
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String nome(Class<?> c) {
        return c.getName().substring(c.getPackageName().length() + 1).replace('$', '_');
    }

    private static String java(Class<?> c) {
        return c.getCanonicalName();
    }

    private void enumeracao(StringBuilder sb, Class<?> c) {
        sb.append("\n    public static ").append(java(c)).append(" ler").append(nome(c)).append("(Object no) {\n")
                .append("        String s = Json.texto(no);\n")
                .append("        return s == null ? null : ").append(java(c)).append(".valueOf(s);\n")
                .append("    }\n");
        sb.append("\n    public static void escrever").append(nome(c)).append("(StringBuilder sb, ").append(java(c))
                .append(" v) {\n        Json.nome(sb, v);\n    }\n");
    }

    private void selado(StringBuilder sb, Class<?> c) {
        sb.append("\n    public static ").append(java(c)).append(" ler").append(nome(c)).append("(Object no) {\n")
                .append("        if (no == null) {\n            return null;\n        }\n")
                .append("        String tipo = Json.texto(Json.objeto(no, \"").append(c.getSimpleName()).append("\").get(\"tipo\"));\n")
                .append("        if (tipo == null) {\n            throw new JsonInvalidoException(\"")
                .append(c.getSimpleName()).append(": falta o campo tipo\");\n        }\n")
                .append("        return switch (tipo) {\n");
        for (Class<?> sub : c.getPermittedSubclasses()) {
            sb.append("            case \"").append(sub.getSimpleName()).append("\" -> ler").append(nome(sub)).append("(no);\n");
        }
        sb.append("            default -> throw new JsonInvalidoException(\"").append(c.getSimpleName())
                .append(": tipo desconhecido \" + tipo);\n        };\n    }\n");
        sb.append("\n    public static void escrever").append(nome(c)).append("(StringBuilder sb, ").append(java(c)).append(" v) {\n")
                .append("        if (v == null) {\n            sb.append(\"null\");\n            return;\n        }\n");
        for (Class<?> sub : c.getPermittedSubclasses()) {
            sb.append("        if (v instanceof ").append(java(sub)).append(" x) {\n")
                    .append("            escrever").append(nome(sub)).append("(sb, x);\n            return;\n        }\n");
        }
        sb.append("        throw new IllegalArgumentException(\"Ordem sem escritor: \" + v);\n    }\n");
    }

    /** {@code tipo} != null: subtipo de um selado, com o campo "tipo" na frente (como o @JsonTypeInfo). */
    private void registro(StringBuilder sb, Class<?> c, String tipo) {
        RecordComponent[] comps = c.getRecordComponents();
        sb.append("\n    public static ").append(java(c)).append(" ler").append(nome(c)).append("(Object no) {\n")
                .append("        if (no == null) {\n            return null;\n        }\n")
                .append("        Map<String, Object> m = Json.objeto(no, \"").append(c.getSimpleName()).append("\");\n")
                .append("        return new ").append(java(c)).append("(");
        List<String> args = new ArrayList<>();
        for (RecordComponent rc : comps) {
            args.add("\n                " + leitor(rc.getGenericType(), "m.get(\"" + rc.getName() + "\")"));
        }
        sb.append(String.join(",", args)).append(");\n    }\n");

        sb.append("\n    public static void escrever").append(nome(c)).append("(StringBuilder sb, ").append(java(c)).append(" v) {\n")
                .append("        if (v == null) {\n            sb.append(\"null\");\n            return;\n        }\n");
        String sep = "{";
        if (tipo != null) {
            sb.append("        sb.append(\"{\\\"tipo\\\":\\\"").append(tipo).append("\\\"\");\n");
            sep = ",";
        }
        for (RecordComponent rc : comps) {
            sb.append("        sb.append(\"").append(sep).append("\\\"").append(rc.getName()).append("\\\":\");\n")
                    .append("        ").append(escritor(rc.getGenericType(), "v." + rc.getName() + "()")).append(";\n");
            sep = ",";
        }
        if (comps.length == 0 && tipo == null) {
            sb.append("        sb.append(\"{\");\n");
        }
        sb.append("        sb.append(\"}\");\n    }\n");
    }

    private String leitor(Type t, String expr) {
        if (t instanceof ParameterizedType p) {
            Class<?> bruto = (Class<?>) p.getRawType();
            Type[] a = p.getActualTypeArguments();
            if (bruto == List.class) {
                return "Json.lista(" + expr + ", o -> " + leitor(a[0], "o") + ")";
            }
            if (bruto == Map.class) {
                return "Json.mapa(" + expr + ", k -> " + chave((Class<?>) a[0]) + ", o -> " + leitor(a[1], "o") + ")";
            }
            throw new IllegalStateException("Tipo generico sem leitor: " + t);
        }
        Class<?> c = (Class<?>) t;
        if (c == int.class) return "Json.inteiro(" + expr + ")";
        if (c == Integer.class) return "Json.inteiroOuNulo(" + expr + ")";
        if (c == long.class) return "Json.longo(" + expr + ")";
        if (c == Long.class) return "Json.longoOuNulo(" + expr + ")";
        if (c == boolean.class) return "Json.logico(" + expr + ")";
        if (c == Boolean.class) return "Json.logicoOuNulo(" + expr + ")";
        if (c == String.class) return "Json.texto(" + expr + ")";
        if (c == BigDecimal.class) return "Json.decimal(" + expr + ")";
        if (c == LocalDate.class) return "Json.data(" + expr + ")";
        if (tipos.contains(c)) return "ler" + nome(c) + "(" + expr + ")";
        throw new IllegalStateException("Tipo sem leitor: " + c);
    }

    private String chave(Class<?> c) {
        if (c == String.class) return "k";
        if (c == Integer.class) return "Integer.valueOf(k)";
        if (c.isEnum()) return java(c) + ".valueOf(k)";
        throw new IllegalStateException("Chave de mapa sem leitor: " + c);
    }

    private String escritor(Type t, String expr) {
        return "(" + lambdaEscritor(t) + ").escrever(sb, " + expr + ")";
    }

    /** Expressao do tipo Json.Escritor&lt;T&gt; para o tipo. */
    private String lambdaEscritor(Type t) {
        if (t instanceof ParameterizedType p) {
            Class<?> bruto = (Class<?>) p.getRawType();
            Type[] a = p.getActualTypeArguments();
            String generico = "Json.Escritor<" + p.getTypeName().replace('$', '.') + ">";
            if (bruto == List.class) {
                return "(" + generico + ") (s, x) -> Json.lista(s, x, " + lambdaEscritor(a[0]) + ")";
            }
            if (bruto == Map.class) {
                return "(" + generico + ") (s, x) -> Json.mapa(s, x, " + lambdaEscritor(a[1]) + ")";
            }
            throw new IllegalStateException("Tipo generico sem escritor: " + t);
        }
        Class<?> c = (Class<?>) t;
        String caixa = c.isPrimitive() ? caixa(c) : java(c);
        String generico = "(Json.Escritor<" + caixa + ">) ";
        if (c == String.class) return generico + "Json::texto";
        if (c == BigDecimal.class) return generico + "Json::decimal";
        if (c == LocalDate.class) return generico + "Json::data";
        if (c == int.class || c == Integer.class || c == long.class || c == Long.class
                || c == boolean.class || c == Boolean.class) {
            return generico + "Json::numero";
        }
        if (tipos.contains(c)) return generico + "CodecGerado::escrever" + nome(c);
        throw new IllegalStateException("Tipo sem escritor: " + c);
    }

    private static String caixa(Class<?> c) {
        if (c == int.class) return "Integer";
        if (c == long.class) return "Long";
        if (c == boolean.class) return "Boolean";
        throw new IllegalStateException("Primitivo sem caixa: " + c);
    }
}
