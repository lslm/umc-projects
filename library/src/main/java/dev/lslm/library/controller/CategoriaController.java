package dev.lslm.library.controller;

import dev.lslm.library.domain.Categoria;
import dev.lslm.library.service.CategoriaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController extends NomeadoController<Categoria> {

    public CategoriaController(CategoriaService service) {
        super(service, "/api/v1/categorias");
    }
}
