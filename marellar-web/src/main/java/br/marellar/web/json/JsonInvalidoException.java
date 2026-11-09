package br.marellar.web.json;

/** JSON mal formado ou com um valor do tipo errado. */
public class JsonInvalidoException extends RuntimeException {
    public JsonInvalidoException(String mensagem) {
        super(mensagem);
    }
}
