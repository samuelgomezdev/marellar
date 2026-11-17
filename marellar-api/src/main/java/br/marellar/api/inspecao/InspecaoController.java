package br.marellar.api.inspecao;

import br.marellar.api.inspecao.CatalogoInspecao.Defeito;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Inspecao 3D: o cliente (pagina three.js em /inspecao/) registra NCs no ponto clicado do barco.
 * O catalogo de defeitos e o ampliado da inspecao (data/nc-catalogo-inspecao.json), ligado ao do motor por {@code base}.
 */
@RestController
@RequestMapping("/api/v1/inspecao")
public class InspecaoController {

    private final InspecaoService service;

    public InspecaoController(InspecaoService service) {
        this.service = service;
    }

    public record DefeitoVisao(String codigo, String descricao, String categoria, String gravidade,
                               List<String> gates, String base, List<String> aplica) {
        static DefeitoVisao de(Defeito d) {
            return new DefeitoVisao(d.codigo(), d.descricao(), d.categoria(), d.gravidade().name(),
                    d.gates().stream().map(Enum::name).toList(), d.base(), d.aplica());
        }
    }

    @GetMapping("/defeitos")
    public List<DefeitoVisao> defeitos() {
        return service.defeitos().stream().map(DefeitoVisao::de).toList();
    }

    @GetMapping("/ncs")
    public List<NcInspecao> listar(@RequestParam(required = false) Integer linha) {
        return service.listar(linha);
    }

    @PostMapping("/ncs")
    @ResponseStatus(HttpStatus.CREATED)
    public NcInspecao registrar(@Valid @RequestBody NovaNcRequest req) {
        return service.registrar(req);
    }

    @PutMapping("/ncs/{id}")
    public NcInspecao editar(@PathVariable String id, @Valid @RequestBody EditarNcRequest req) {
        return service.editar(id, req);
    }

    @PostMapping("/ncs/{id}/fechar")
    public NcInspecao fechar(@PathVariable String id) {
        return service.fechar(id);
    }

    @DeleteMapping("/ncs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        service.excluir(id);
    }
}
