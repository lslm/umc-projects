package dev.lslm.library.controller;

import dev.lslm.library.dto.AcervoDtos.CatalogoItem;
import dev.lslm.library.dto.PageResponse;
import dev.lslm.library.service.CatalogoService;
import dev.lslm.library.support.Paginacao;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** RF06 - pesquisa de catalogo. */
@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogoController {

    private final CatalogoService service;

    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping("/busca")
    public PageResponse<CatalogoItem> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) Boolean disponivel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "20") int perPage) {
        return service.buscar(q, titulo, autor, isbn, categoriaId, disponivel,
                Paginacao.de(page, perPage, Sort.by("title")));
    }
}
