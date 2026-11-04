package dev.lslm.library.controller;

import dev.lslm.library.dto.RelatorioDtos.Atraso;
import dev.lslm.library.dto.RelatorioDtos.Circulacao;
import dev.lslm.library.dto.RelatorioDtos.LeitorAtivo;
import dev.lslm.library.dto.RelatorioDtos.ObraEmprestada;
import dev.lslm.library.service.RelatorioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** RF15 - relatorios. Parametros de periodo: de/ate em ISO 8601 UTC (padrao: ultimos 30 dias). */
@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/circulacao")
    public Circulacao circulacao(@RequestParam(required = false) Instant de,
                                 @RequestParam(required = false) Instant ate) {
        return service.circulacao(de, ate);
    }

    @GetMapping("/atrasos")
    public List<Atraso> atrasos() {
        return service.atrasos();
    }

    @GetMapping("/leitores-ativos")
    public List<LeitorAtivo> leitoresAtivos(@RequestParam(required = false) Instant de,
                                            @RequestParam(required = false) Instant ate) {
        return service.leitoresAtivos(de, ate);
    }

    @GetMapping("/obras-mais-emprestadas")
    public List<ObraEmprestada> obrasMaisEmprestadas(@RequestParam(required = false) Instant de,
                                                     @RequestParam(required = false) Instant ate,
                                                     @RequestParam(defaultValue = "10") int limite) {
        return service.obrasMaisEmprestadas(de, ate, limite);
    }
}
