package br.marellar.engine.dados;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;

/**
 * Textos para o jogador (arquivo data/textos-ptbr.json).
 * Guarda a arvore JSON e busca por caminho com pontos, ex.: "eventos.gateAprovado".
 */
public final class Textos {

    private final JsonNode raiz;

    public Textos(JsonNode raiz) {
        this.raiz = Objects.requireNonNull(raiz);
    }

    public JsonNode no(String caminho) {
        JsonNode atual = raiz;
        for (String parte : caminho.split("\\.")) {
            atual = atual.path(parte);
        }
        return atual;
    }

    public boolean existe(String caminho) {
        return !no(caminho).isMissingNode();
    }

    /** Texto simples, com {chave} trocada pelos valores informados. */
    public String formatar(String caminho, Map<String, String> valores) {
        JsonNode no = no(caminho);
        if (!no.isTextual()) {
            throw new IllegalArgumentException("Texto nao encontrado: " + caminho);
        }
        String texto = no.asText();
        for (Map.Entry<String, String> e : valores.entrySet()) {
            texto = texto.replace("{" + e.getKey() + "}", e.getValue());
        }
        return texto;
    }

    /** Uma das falas de uma lista, escolhida pelo indice (o sorteio fica com quem chama). */
    public String fala(String caminho, int indice) {
        JsonNode lista = no(caminho);
        if (!lista.isArray() || lista.isEmpty()) {
            throw new IllegalArgumentException("Lista de falas nao encontrada: " + caminho);
        }
        return lista.get(Math.floorMod(indice, lista.size())).asText();
    }

    public int quantidadeFalas(String caminho) {
        JsonNode lista = no(caminho);
        return lista.isArray() ? lista.size() : 0;
    }
}
