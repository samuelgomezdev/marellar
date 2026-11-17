package br.marellar.api.inspecao;

import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Gravidade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo do PUT que edita uma NC aberta: tudo pode mudar, inclusive o ponto (peca e posicao),
 * menos a linha, o numero e a data de registro.
 */
public record EditarNcRequest(
        @NotNull NcInspecao.Peca peca,
        @NotNull @Valid NcInspecao.Posicao posicao,
        @Min(0) @Max(15) int baliza,
        @NotNull NcInspecao.Bordo bordo,
        @Size(max = 40) String zona,
        @Size(max = 10) String compartimento,
        @Size(max = 40) String componente,
        @NotNull CodigoGate gate,
        @NotBlank String codigoDefeito,
        @NotNull Gravidade gravidade,
        @Size(max = 300) String observacao) {}
