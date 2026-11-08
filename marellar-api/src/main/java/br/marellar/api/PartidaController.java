package br.marellar.api;

import br.marellar.engine.ResultadoAvanco;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dominio.Evento;
import br.marellar.engine.estado.EstadoPartida;
import br.marellar.engine.estado.EstadoPartida.BarcoVisao;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;
import br.marellar.engine.relatorio.RelatorioConselho;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rotas do README, secao 7. A API nao calcula regra nenhuma: so repassa ao motor.
 */
@RestController
@RequestMapping("/api/v1/partidas")
public class PartidaController {

    /** Limite por chamada: 1 mes de jogo. */
    static final int MAX_TICKS_POR_CHAMADA = 168;

    private final PartidaService service;

    public PartidaController(PartidaService service) {
        this.service = service;
    }

    public record CriarPartidaRequest(@NotNull Long semente) {}

    public record CriarPartidaResponse(String id) {}

    public record AvancarRequest(@Min(0) @Max(MAX_TICKS_POR_CHAMADA) int ticks) {}

    public record AvancarResponse(ResultadoAvanco resultado, EstadoPartida estado) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CriarPartidaResponse criar(@Valid @RequestBody CriarPartidaRequest req) {
        return new CriarPartidaResponse(service.criar(req.semente()));
    }

    @GetMapping("/{id}")
    public EstadoPartida estado(@PathVariable String id) {
        return service.buscar(id).estado();
    }

    @PostMapping("/{id}/avancar")
    public AvancarResponse avancar(@PathVariable String id, @Valid @RequestBody AvancarRequest req) {
        Simulacao sim = service.buscar(id);
        ResultadoAvanco resultado = sim.avancar(req.ticks());
        return new AvancarResponse(resultado, sim.estado());
    }

    @PostMapping("/{id}/ordens")
    public EstadoPartida ordem(@PathVariable String id, @RequestBody Ordem ordem) {
        Simulacao sim = service.buscar(id);
        sim.aplicarOrdem(ordem);
        return sim.estado();
    }

    @GetMapping("/{id}/barcos/{barcoId}")
    public BarcoVisao barco(@PathVariable String id, @PathVariable String barcoId) {
        return service.buscar(id).barco(barcoId);
    }

    @GetMapping("/{id}/conselho")
    public RelatorioConselho conselho(@PathVariable String id) {
        return service.buscar(id).conselho()
                .orElseThrow(() -> new OrdemInvalidaException("Ainda nao houve reuniao do conselho nesta partida"));
    }

    @PostMapping("/{id}/salvar")
    public PartidaService.ResumoSave salvar(@PathVariable String id) {
        return service.salvar(id);
    }

    public record CarregarRequest(@NotNull String id) {}

    @PostMapping("/carregar")
    public CriarPartidaResponse carregar(@Valid @RequestBody CarregarRequest req) {
        return new CriarPartidaResponse(service.carregar(req.id()));
    }

    @GetMapping("/saves")
    public List<PartidaService.ResumoSave> saves() {
        return service.listarSaves();
    }

    @GetMapping("/{id}/eventos")
    public List<Evento> eventos(@PathVariable String id, @RequestParam(defaultValue = "0") long desde) {
        return service.buscar(id).eventosDesde(desde);
    }
}
