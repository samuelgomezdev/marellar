package br.marellar.api.inspecao;

import br.marellar.engine.dados.CatalogoNc.TipoNc;
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
 * O catalogo de defeitos e o mesmo do motor (data/nc-catalogo.json).
 */
@RestController
@RequestMapping("/api/v1/inspecao")
public class InspecaoController {

    private final InspecaoService service;

    public InspecaoController(InspecaoService service) {
        this.service = service;
    }

    public record DefeitoVisao(String codigo, String descricao, String gravidade, List<String> gates) {
        static DefeitoVisao de(TipoNc t) {
            return new DefeitoVisao(t.codigo(), t.descricao(), t.gravidade().name(),
                    t.gates().stream().map(Enum::name).toList());
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
