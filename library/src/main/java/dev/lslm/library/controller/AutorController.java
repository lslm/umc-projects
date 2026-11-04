package dev.lslm.library.controller;

import dev.lslm.library.domain.Autor;
import dev.lslm.library.service.AutorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/autores")
public class AutorController extends NomeadoController<Autor> {

    public AutorController(AutorService service) {
        super(service, "/api/v1/autores");
    }
}
