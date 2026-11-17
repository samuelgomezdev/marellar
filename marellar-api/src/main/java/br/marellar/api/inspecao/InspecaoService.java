package br.marellar.api.inspecao;

import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.CatalogoNc;
import br.marellar.engine.dados.CatalogoNc.TipoNc;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * NCs da inspecao 3D. Ficam em memoria e num arquivo JSON ({@code inspecao/ncs.json} por padrao),
 * gravado a cada mudanca por troca de arquivo temporario, como os saves.
 *
 * <p>Regra de gravidade: o inspetor pode subir a gravidade do catalogo (uma trinca leve que ele julga media),
 * mas nao pode baixar. Assim um defeito que o catalogo classifica como critico nunca entra como leve.</p>
 */
@Service
public class InspecaoService {

    static final Set<Integer> LINHAS = Set.of(33, 47, 59);

    private final CatalogoNc catalogo = CarregadorDados.carregarDoClasspath().catalogoNc();
    private final ObjectMapper json;
    private final Path arquivo;
    private final Clock relogio;
    private final List<NcInspecao> ncs = new ArrayList<>();

    @Autowired
    public InspecaoService(ObjectMapper json,
                           @Value("${marellar.inspecao.arquivo:inspecao/ncs.json}") String arquivo) {
        this(json, Path.of(arquivo), Clock.systemUTC());
    }

    InspecaoService(ObjectMapper json, Path arquivo, Clock relogio) {
        this.json = json;
        this.arquivo = arquivo;
        this.relogio = relogio;
        carregar();
    }

    public List<TipoNc> defeitos() {
        return catalogo.tipos();
    }

    public synchronized List<NcInspecao> listar(Integer linha) {
        return ncs.stream()
                .filter(n -> linha == null || n.linha() == linha)
                .sorted(Comparator.comparingInt(NcInspecao::linha).thenComparingInt(NcInspecao::numero))
                .toList();
    }

    public synchronized NcInspecao registrar(NovaNcRequest req) {
        if (!LINHAS.contains(req.linha())) {
            throw new NcInvalidaException("Linha desconhecida: " + req.linha() + " (use 33, 47 ou 59)");
        }
        TipoNc tipo = tipoValido(req.codigoDefeito(), req.gravidade());
        int numero = ncs.stream().filter(n -> n.linha() == req.linha()).mapToInt(NcInspecao::numero).max().orElse(0) + 1;
        NcInspecao nc = new NcInspecao(UUID.randomUUID().toString(), numero, req.linha(), req.peca(), req.posicao(),
                req.baliza(), req.bordo(), req.zona(), req.componente(), req.gate(), tipo.codigo(), tipo.descricao(), req.gravidade(),
                req.observacao() == null ? "" : req.observacao().strip(), NcInspecao.Status.ABERTA,
                Instant.now(relogio), null, null);
        ncs.add(nc);
        gravar();
        return nc;
    }

    /** Edita uma NC aberta. Linha, numero e data de registro nao mudam. */
    public synchronized NcInspecao editar(String id, EditarNcRequest req) {
        NcInspecao atual = buscar(id);
        if (atual.status() == NcInspecao.Status.FECHADA) {
            throw new NcInvalidaException("A NC " + atual.numero() + " esta fechada e nao pode ser editada");
        }
        TipoNc tipo = tipoValido(req.codigoDefeito(), req.gravidade());
        NcInspecao editada = new NcInspecao(atual.id(), atual.numero(), atual.linha(), req.peca(), req.posicao(),
                req.baliza(), req.bordo(), req.zona(), req.componente(), req.gate(), tipo.codigo(), tipo.descricao(),
                req.gravidade(), req.observacao() == null ? "" : req.observacao().strip(), NcInspecao.Status.ABERTA,
                atual.registradaEm(), Instant.now(relogio), null);
        ncs.set(ncs.indexOf(atual), editada);
        gravar();
        return editada;
    }

    /** Defeito tem de existir no catalogo; a gravidade pode subir, mas nao pode ficar abaixo da do catalogo. */
    private TipoNc tipoValido(String codigo, br.marellar.engine.dominio.Gravidade gravidade) {
        TipoNc tipo = catalogo.tipos().stream().filter(t -> t.codigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new NcInvalidaException("Defeito fora do catalogo: " + codigo));
        if (gravidade.compareTo(tipo.gravidade()) < 0) {
            throw new NcInvalidaException("O catalogo classifica " + tipo.codigo() + " como " + tipo.gravidade()
                    + "; a gravidade pode subir, mas nao pode ficar abaixo disso");
        }
        return tipo;
    }

    public synchronized NcInspecao fechar(String id) {
        NcInspecao atual = buscar(id);
        if (atual.status() == NcInspecao.Status.FECHADA) {
            return atual;
        }
        NcInspecao fechada = atual.fechar(Instant.now(relogio));
        ncs.set(ncs.indexOf(atual), fechada);
        gravar();
        return fechada;
    }

    public synchronized void excluir(String id) {
        ncs.remove(buscar(id));
        gravar();
    }

    private NcInspecao buscar(String id) {
        return ncs.stream().filter(n -> n.id().equals(id)).findFirst()
                .orElseThrow(() -> new NcNaoEncontradaException(id));
    }

    // ----- arquivo -----

    private void carregar() {
        if (!Files.isRegularFile(arquivo)) {
            return;
        }
        try {
            ncs.addAll(json.readValue(arquivo.toFile(), new TypeReference<List<NcInspecao>>() {}));
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler " + arquivo, e);
        }
    }

    private void gravar() {
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            Files.createDirectories(pasta);
            Path temporario = Files.createTempFile(pasta, "ncs", ".tmp");
            json.writerWithDefaultPrettyPrinter().writeValue(temporario.toFile(), ncs);
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel gravar " + arquivo, e);
        }
    }

    public static class NcNaoEncontradaException extends RuntimeException {
        public NcNaoEncontradaException(String id) {
            super("NC nao encontrada: " + id);
        }
    }

    public static class NcInvalidaException extends RuntimeException {
        public NcInvalidaException(String mensagem) {
            super(mensagem);
        }
    }
}
