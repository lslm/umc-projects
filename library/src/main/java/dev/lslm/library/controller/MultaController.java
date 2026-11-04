package dev.lslm.library.controller;

import dev.lslm.library.domain.Multa.Status;
import dev.lslm.library.dto.CirculacaoDtos.AjusteMultaRequest;
import dev.lslm.library.dto.CirculacaoDtos.JustificativaRequest;
import dev.lslm.library.dto.CirculacaoDtos.MultaResponse;
import dev.lslm.library.dto.PageResponse;
import dev.lslm.library.service.MultaService;
import dev.lslm.library.support.Paginacao;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** RF11 - multas e pendencias. */
@RestController
@RequestMapping("/api/v1/multas")
public class MultaController {

    private final MultaService service;

    public MultaController(MultaService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<MultaResponse> listar(@RequestParam(required = false) UUID leitorId,
                                              @RequestParam(required = false) Status status,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(name = "per_page", defaultValue = "20") int perPage) {
        return PageResponse.of(service.listar(leitorId, status, Paginacao.de(page, perPage,
                Sort.by("criadaEm").descending())), MultaResponse::from);
    }

    @GetMapping("/{id}")
    public MultaResponse buscar(@PathVariable UUID id) {
        return MultaResponse.from(service.buscar(id));
    }

    @PutMapping("/{id}/ajuste")
    public MultaResponse ajustar(@PathVariable UUID id, @RequestBody AjusteMultaRequest req) {
        return MultaResponse.from(service.ajustar(id, req.valor(), req.justificativa()));
    }

    @PostMapping("/{id}/isencao")
    public MultaResponse isentar(@PathVariable UUID id, @RequestBody JustificativaRequest req) {
        return MultaResponse.from(service.isentar(id, req.justificativa()));
    }

    @PostMapping("/{id}/pagamento")
    public MultaResponse pagar(@PathVariable UUID id, @RequestBody(required = false) JustificativaRequest req) {
        return MultaResponse.from(service.pagar(id, req != null ? req.justificativa() : null));
    }
}
