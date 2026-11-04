package dev.lslm.library.controller;

import dev.lslm.library.domain.Reserva.Status;
import dev.lslm.library.dto.CirculacaoDtos.ReservaRequest;
import dev.lslm.library.dto.CirculacaoDtos.ReservaResponse;
import dev.lslm.library.dto.PageResponse;
import dev.lslm.library.service.ReservaService;
import dev.lslm.library.support.Paginacao;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** RF12 - reservas e fila de espera. A listagem vem ordenada pela ordem de chegada (fila). */
@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> criar(@RequestBody ReservaRequest req) {
        ReservaResponse r = service.criar(req.livroId(), req.leitorId());
        return ResponseEntity.created(URI.create("/api/v1/reservas/" + r.id())).body(r);
    }

    @GetMapping
    public PageResponse<ReservaResponse> listar(@RequestParam(required = false) UUID livroId,
                                                @RequestParam(required = false) UUID leitorId,
                                                @RequestParam(required = false) Status status,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(name = "per_page", defaultValue = "20") int perPage) {
        var p = service.listar(livroId, leitorId, status, Paginacao.de(page, perPage, Sort.by("criadaEm")));
        return PageResponse.ofList(service.toResponses(p.getContent()), p);
    }

    @GetMapping("/{id}")
    public ReservaResponse buscar(@PathVariable UUID id) {
        return service.detalhar(id);
    }

    @DeleteMapping("/{id}")
    public ReservaResponse cancelar(@PathVariable UUID id) {
        return service.cancelar(id);
    }

    @PostMapping("/{id}/cancelamento")
    public ReservaResponse cancelarPost(@PathVariable UUID id) {
        return service.cancelar(id);
    }
}
