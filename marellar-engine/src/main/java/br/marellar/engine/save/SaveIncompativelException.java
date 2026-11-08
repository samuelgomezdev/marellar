package br.marellar.engine.save;

/** O save nao pode ser carregado: versao desconhecida ou parametros de data/ diferentes. */
public class SaveIncompativelException extends RuntimeException {

    public SaveIncompativelException(String mensagem) {
        super(mensagem);
    }

    public SaveIncompativelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
