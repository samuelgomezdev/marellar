package br.marellar.web.json;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * JSON sem reflexao, para o motor rodar no navegador (TeaVM nao roda o Jackson).
 * Le para uma arvore simples: objeto = {@link LinkedHashMap}, lista = {@link ArrayList},
 * numero = {@link BigDecimal}, texto = {@link String}, {@link Boolean} e {@code null}.
 * Os conversores de cada record ficam em {@link CodecGerado}, gerado a partir dos records.
 */
public final class Json {

    private Json() {
    }

    // ----- leitura -----

    public static Object ler(String texto) {
        Leitor l = new Leitor(texto);
        l.espacos();
        Object valor = l.valor();
        l.espacos();
        if (l.pos != texto.length()) {
            throw l.erro("conteudo depois do fim do JSON");
        }
        return valor;
    }

    private static final class Leitor {
        private final String s;
        private int pos;

        Leitor(String s) {
            this.s = s;
        }

        JsonInvalidoException erro(String msg) {
            return new JsonInvalidoException(msg + " (posicao " + pos + ")");
        }

        void espacos() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                    return;
                }
                pos++;
            }
        }

        Object valor() {
            if (pos >= s.length()) {
                throw erro("fim inesperado");
            }
            char c = s.charAt(pos);
            switch (c) {
                case '{':
                    return objeto();
                case '[':
                    return lista();
                case '"':
                    return texto();
                case 't':
                    palavra("true");
                    return Boolean.TRUE;
                case 'f':
                    palavra("false");
                    return Boolean.FALSE;
                case 'n':
                    palavra("null");
                    return null;
                default:
                    return numero();
            }
        }

        void palavra(String p) {
            if (!s.startsWith(p, pos)) {
                throw erro("esperava " + p);
            }
            pos += p.length();
        }

        Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<>();
            pos++;
            espacos();
            if (pos < s.length() && s.charAt(pos) == '}') {
                pos++;
                return m;
            }
            while (true) {
                espacos();
                if (pos >= s.length() || s.charAt(pos) != '"') {
                    throw erro("esperava o nome de um campo");
                }
                String chave = texto();
                espacos();
                esperar(':');
                espacos();
                m.put(chave, valor());
                espacos();
                if (pos < s.length() && s.charAt(pos) == ',') {
                    pos++;
                    continue;
                }
                esperar('}');
                return m;
            }
        }

        List<Object> lista() {
            List<Object> l = new ArrayList<>();
            pos++;
            espacos();
            if (pos < s.length() && s.charAt(pos) == ']') {
                pos++;
                return l;
            }
            while (true) {
                espacos();
                l.add(valor());
                espacos();
                if (pos < s.length() && s.charAt(pos) == ',') {
                    pos++;
                    continue;
                }
                esperar(']');
                return l;
            }
        }

        void esperar(char c) {
            if (pos >= s.length() || s.charAt(pos) != c) {
                throw erro("esperava '" + c + "'");
            }
            pos++;
        }

        String texto() {
            pos++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= s.length()) {
                    throw erro("texto sem fim");
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (pos >= s.length()) {
                    throw erro("escape sem fim");
                }
                char e = s.charAt(pos++);
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > s.length()) {
                            throw erro("escape \\u incompleto");
                        }
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw erro("escape invalido \\" + e);
                }
            }
        }

        BigDecimal numero() {
            int inicio = pos;
            while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) {
                pos++;
            }
            if (inicio == pos) {
                throw erro("valor invalido");
            }
            try {
                return new BigDecimal(s.substring(inicio, pos));
            } catch (NumberFormatException e) {
                throw erro("numero invalido");
            }
        }
    }

    // ----- conversao da arvore (usada pelo CodecGerado) -----

    @SuppressWarnings("unchecked")
    public static Map<String, Object> objeto(Object no, String tipo) {
        if (no instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        throw new JsonInvalidoException(tipo + ": esperava um objeto");
    }

    public static String texto(Object no) {
        if (no == null || no instanceof String) {
            return (String) no;
        }
        if (no instanceof BigDecimal d) {
            return d.toPlainString();
        }
        if (no instanceof Boolean b) {
            return b.toString();
        }
        throw new JsonInvalidoException("esperava um texto");
    }

    public static BigDecimal decimal(Object no) {
        if (no == null || no instanceof BigDecimal) {
            return (BigDecimal) no;
        }
        if (no instanceof String s) {
            return new BigDecimal(s);
        }
        throw new JsonInvalidoException("esperava um numero");
    }

    public static Integer inteiroOuNulo(Object no) {
        BigDecimal d = decimal(no);
        return d == null ? null : d.intValueExact();
    }

    public static int inteiro(Object no) {
        Integer i = inteiroOuNulo(no);
        return i == null ? 0 : i;
    }

    public static Long longoOuNulo(Object no) {
        BigDecimal d = decimal(no);
        return d == null ? null : d.longValueExact();
    }

    public static long longo(Object no) {
        Long l = longoOuNulo(no);
        return l == null ? 0L : l;
    }

    public static Boolean logicoOuNulo(Object no) {
        if (no == null || no instanceof Boolean) {
            return (Boolean) no;
        }
        if (no instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        throw new JsonInvalidoException("esperava true ou false");
    }

    public static boolean logico(Object no) {
        Boolean b = logicoOuNulo(no);
        return b != null && b;
    }

    public static LocalDate data(Object no) {
        String s = texto(no);
        return s == null ? null : LocalDate.parse(s);
    }

    public static <T> List<T> lista(Object no, Function<Object, T> item) {
        if (no == null) {
            return null;
        }
        if (!(no instanceof List<?> l)) {
            throw new JsonInvalidoException("esperava uma lista");
        }
        List<T> saida = new ArrayList<>(l.size());
        for (Object o : l) {
            saida.add(item.apply(o));
        }
        return saida;
    }

    public static <K, V> Map<K, V> mapa(Object no, Function<String, K> chave, Function<Object, V> valor) {
        if (no == null) {
            return null;
        }
        Map<String, Object> m = objeto(no, "mapa");
        Map<K, V> saida = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : m.entrySet()) {
            saida.put(chave.apply(e.getKey()), valor.apply(e.getValue()));
        }
        return saida;
    }

    // ----- escrita (mesmo formato compacto do Jackson) -----

    public static void texto(StringBuilder sb, String s) {
        if (s == null) {
            sb.append("null");
            return;
        }
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c).toUpperCase();
                        sb.append("\\u00").append(hex.length() == 1 ? "0" : "").append(hex);
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    public static void decimal(StringBuilder sb, BigDecimal d) {
        sb.append(d == null ? "null" : d.toString());
    }

    public static void numero(StringBuilder sb, Object n) {
        sb.append(n == null ? "null" : n.toString());
    }

    public static void data(StringBuilder sb, LocalDate d) {
        texto(sb, d == null ? null : d.toString());
    }

    public static void nome(StringBuilder sb, Enum<?> e) {
        texto(sb, e == null ? null : e.name());
    }

    public interface Escritor<T> {
        void escrever(StringBuilder sb, T valor);
    }

    public static <T> void lista(StringBuilder sb, Collection<T> l, Escritor<T> item) {
        if (l == null) {
            sb.append("null");
            return;
        }
        sb.append('[');
        boolean primeiro = true;
        for (T v : l) {
            if (!primeiro) {
                sb.append(',');
            }
            primeiro = false;
            item.escrever(sb, v);
        }
        sb.append(']');
    }

    public static <K, V> void mapa(StringBuilder sb, Map<K, V> m, Escritor<V> valor) {
        if (m == null) {
            sb.append("null");
            return;
        }
        sb.append('{');
        boolean primeiro = true;
        for (Map.Entry<K, V> e : m.entrySet()) {
            if (!primeiro) {
                sb.append(',');
            }
            primeiro = false;
            K k = e.getKey();
            texto(sb, k instanceof Enum<?> en ? en.name() : String.valueOf(k));
            sb.append(':');
            valor.escrever(sb, e.getValue());
        }
        sb.append('}');
    }

    /** Escreve uma arvore lida por {@link #ler(String)} (usado pelos textos e pelas respostas simples). */
    public static void arvore(StringBuilder sb, Object no) {
        if (no == null) {
            sb.append("null");
        } else if (no instanceof String s) {
            texto(sb, s);
        } else if (no instanceof Map<?, ?> m) {
            sb.append('{');
            boolean primeiro = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!primeiro) {
                    sb.append(',');
                }
                primeiro = false;
                texto(sb, String.valueOf(e.getKey()));
                sb.append(':');
                arvore(sb, e.getValue());
            }
            sb.append('}');
        } else if (no instanceof Collection<?> l) {
            sb.append('[');
            boolean primeiro = true;
            for (Object o : l) {
                if (!primeiro) {
                    sb.append(',');
                }
                primeiro = false;
                arvore(sb, o);
            }
            sb.append(']');
        } else if (no instanceof Enum<?> e) {
            texto(sb, e.name());
        } else {
            sb.append(no);
        }
    }

    /** Converte a arvore lida em Map/List/String do jeito que o {@code Textos} espera. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> arvoreDeTextos(Object no) {
        return (Map<String, Object>) no;
    }
}
