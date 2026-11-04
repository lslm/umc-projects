package dev.lslm.library.controller;

import dev.lslm.library.domain.Emprestimo.Status;
import dev.lslm.library.dto.CirculacaoDtos.EmprestimoResponse;
import dev.lslm.library.dto.LeitorDtos.AreaLeitorResponse;
import dev.lslm.library.dto.LeitorDtos.BloqueioRequest;
import dev.lslm.library.dto.LeitorDtos.DispositivoRequest;
import dev.lslm.library.dto.LeitorDtos.LeitorRequest;
import dev.lslm.library.dto.LeitorDtos.LeitorResponse;
import dev.lslm.library.dto.NotificacaoDtos.NotificacaoResponse;
import dev.lslm.library.dto.PageResponse;
import dev.lslm.library.service.AreaLeitorService;
import dev.lslm.library.service.LeitorService;
import dev.lslm.library.service.NotificacaoService;
import dev.lslm.library.support.Paginacao;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** RF07 (leitores) e RF14 (area do leitor). */
@RestController
@RequestMapping("/api/v1/leitores")
public class LeitorController {

    private final LeitorService service;
    private final AreaLeitorService areaService;
    private final NotificacaoService notificacaoService;

    public LeitorController(LeitorService service, AreaLeitorService areaService,
                            NotificacaoService notificacaoService) {
        this.service = service;
        this.areaService = areaService;
        this.notificacaoService = notificacaoService;
    }

    @PostMapping
    public ResponseEntity<LeitorResponse> criar(@RequestBody LeitorRequest req) {
        LeitorResponse r = LeitorResponse.from(service.criar(req));
        return ResponseEntity.created(URI.create("/api/v1/leitores/" + r.id())).body(r);
    }

    @GetMapping
    public PageResponse<LeitorResponse> listar(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) Boolean bloqueado,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(name = "per_page", defaultValue = "20") int perPage) {
        return PageResponse.of(service.listar(q, bloqueado, Paginacao.de(page, perPage, Sort.by("nome"))),
                LeitorResponse::from);
    }

    @GetMapping("/{id}")
    public LeitorResponse buscar(@PathVariable UUID id) {
        return LeitorResponse.from(service.buscar(id));
    }

    @PutMapping("/{id}")
    public LeitorResponse atualizar(@PathVariable UUID id, @RequestBody LeitorRequest req) {
        return LeitorResponse.from(service.atualizar(id, req));
    }

    @PostMapping("/{id}/bloqueio")
    public LeitorResponse bloquear(@PathVariable UUID id, @RequestBody BloqueioRequest req) {
        return LeitorResponse.from(service.bloquear(id, req.motivo()));
    }

    @PostMapping("/{id}/desbloqueio")
    public LeitorResponse desbloquear(@PathVariable UUID id) {
        return LeitorResponse.from(service.desbloquear(id));
    }

    @PutMapping("/{id}/dispositivo")
    public LeitorResponse registrarDispositivo(@PathVariable UUID id, @RequestBody DispositivoRequest req) {
        return LeitorResponse.from(service.registrarDispositivo(id, req.fcmToken()));
    }

    /** Historico e emprestimos atuais do leitor (filtre por status=ATIVO ou DEVOLVIDO). */
    @GetMapping("/{id}/emprestimos")
    public List<EmprestimoResponse> emprestimos(@PathVariable UUID id, @RequestParam(required = false) Status status) {
        return areaService.emprestimos(id, status);
    }

    @GetMapping("/{id}/notificacoes")
    public PageResponse<NotificacaoResponse> notificacoes(@PathVariable UUID id,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(name = "per_page", defaultValue = "20") int perPage) {
        return PageResponse.of(notificacaoService.listar(id, Paginacao.de(page, perPage, Sort.unsorted())),
                NotificacaoResponse::from);
    }

    @GetMapping("/{id}/area")
    public AreaLeitorResponse area(@PathVariable UUID id) {
        return areaService.area(id);
    }
}
