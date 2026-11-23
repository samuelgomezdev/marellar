package br.marellar.engine.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * NC marcada pelo jogador no modelo 3D do barco (inspecao dentro do jogo). Fica na partida e vai no save
 * (o save regrava a ordem que a criou). Por enquanto so registra: nao gera retrabalho nem custo.
 *
 * <p>{@code item} e o codigo do item da estrutura de produto clicado (ex.: VTR-33-011); {@code compartimento} o
 * ambiente (ex.: CONS). A posicao (x, y, z) e em metros no sistema do barco: x da popa para a proa, y para cima a
 * partir da quilha, z para boreste.</p>
 */
public record NcInspecao3d(
        String id,
        int numero,
        String barcoId,
        CodigoGate gate,
        String codigoDefeito,
        String codigoBase,
        String categoria,
        String descricao,
        Gravidade gravidade,
        String item,
        String compartimento,
        BigDecimal x,
        BigDecimal y,
        BigDecimal z,
        String observacao,
        LocalDate data,
        int hora) {}
