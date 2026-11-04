package dev.lslm.library.controller;

import dev.lslm.library.dto.AuditoriaResponse;
import dev.lslm.library.dto.PageResponse;
import dev.lslm.library.service.AuditService;
import dev.lslm.library.support.Paginacao;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** RF16 - consulta da trilha de auditoria da biblioteca. */
@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {

    private final AuditService service;

    public AuditoriaController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AuditoriaResponse> listar(@RequestParam(required = false) String acao,
                                                  @RequestParam(required = false) String entidade,
                                                  @RequestParam(required = false) UUID usuarioId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(name = "per_page", defaultValue = "20") int perPage) {
        return PageResponse.of(service.buscar(acao, entidade, usuarioId,
                Paginacao.de(page, perPage, Sort.by("criadaEm").descending())), AuditoriaResponse::from);
    }
}
