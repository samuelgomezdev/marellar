package br.marellar.engine.dominio;

/**
 * Gates da linha de 33 pes usados no MVP, na ordem em que o barco passa por eles.
 * Sem RE: o casco nasce na fabrica; recebimento e inspecao de material comprado, nao etapa do barco
 * (DEC-0054 da wiki). Ordem e nomes do Documento de Visao: PI, TR, TM (DEC-0056).
 */
public enum CodigoGate {
    DM, BA, BF, ST, CT, PI, TR, TM, ET;

    public boolean ultimo() {
        return this == values()[values().length - 1];
    }

    public CodigoGate proximo() {
        if (ultimo()) {
            throw new IllegalStateException("ET e o ultimo gate");
        }
        return values()[ordinal() + 1];
    }
}
