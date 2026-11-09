package br.marellar.engine.dados;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Textos para o jogador (arquivo data/textos-ptbr.json).
 * Guarda a arvore JSON ja lida (objeto = {@link Map}, lista = {@link List}, texto = {@link String})
 * e busca por caminho com pontos, ex.: "eventos.gateAprovado". A arvore nao depende de biblioteca
 * de JSON: quem carrega (Jackson na JVM, o leitor proprio no navegador) so entrega Map/List/String.
 */
public final class Textos {

    private final Map<String, Object> raiz;

    public Textos(Map<String, Object> raiz) {
        this.raiz = Objects.requireNonNull(raiz);
    }

    /** No do caminho, ou {@code null} se nao existe. */
    public Object no(String caminho) {
        Object atual = raiz;
        for (String parte : caminho.split("\\.")) {
            if (!(atual instanceof Map<?, ?> mapa)) {
                return null;
            }
            atual = mapa.get(parte);
        }
        return atual;
    }

    public boolean existe(String caminho) {
        return no(caminho) != null;
    }

    public boolean ehTexto(String caminho) {
        return no(caminho) instanceof String;
    }

    public boolean ehListaNaoVazia(String caminho) {
        return no(caminho) instanceof List<?> lista && !lista.isEmpty();
    }

    /** Texto simples, com {chave} trocada pelos valores informados. */
    public String formatar(String caminho, Map<String, String> valores) {
        if (!(no(caminho) instanceof String texto)) {
            throw new IllegalArgumentException("Texto nao encontrado: " + caminho);
        }
        for (Map.Entry<String, String> e : valores.entrySet()) {
            texto = texto.replace("{" + e.getKey() + "}", e.getValue());
        }
        return texto;
    }

    /** Uma das falas de uma lista, escolhida pelo indice (o sorteio fica com quem chama). */
    public String fala(String caminho, int indice) {
        if (!(no(caminho) instanceof List<?> lista) || lista.isEmpty()) {
            throw new IllegalArgumentException("Lista de falas nao encontrada: " + caminho);
        }
        return String.valueOf(lista.get(Math.floorMod(indice, lista.size())));
    }

    public int quantidadeFalas(String caminho) {
        return no(caminho) instanceof List<?> lista ? lista.size() : 0;
    }

    /** Campo de um objeto de uma lista, ex.: campo("conselheiros", 0, "nome"). */
    public String campo(String caminhoLista, int indice, String campo) {
        if (!(no(caminhoLista) instanceof List<?> lista) || indice >= lista.size()
                || !(lista.get(indice) instanceof Map<?, ?> objeto) || !(objeto.get(campo) instanceof String texto)) {
            throw new IllegalArgumentException("Texto nao encontrado: " + caminhoLista + "[" + indice + "]." + campo);
        }
        return texto;
    }
}
