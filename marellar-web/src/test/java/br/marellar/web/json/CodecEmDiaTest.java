package br.marellar.web.json;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/** O CodecGerado.java tem que estar em dia com os records do motor. */
class CodecEmDiaTest {

    @Test
    void codecGeradoEstaEmDia() throws Exception {
        String esperado = GeradorCodec.gerar();
        if (Boolean.getBoolean("marellar.gerarCodec")) {
            Files.writeString(GeradorCodec.ARQUIVO, esperado, StandardCharsets.UTF_8);
        }
        String atual = Files.readString(GeradorCodec.ARQUIVO, StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertThat(atual)
                .as("CodecGerado.java desatualizado: rode mvnw -pl marellar-web test -Dtest=CodecEmDiaTest -Dmarellar.gerarCodec=true")
                .isEqualTo(esperado);
    }
}
