package dev.lslm.library.controller;

import dev.lslm.library.domain.Editora;
import dev.lslm.library.service.EditoraService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/editoras")
public class EditoraController extends NomeadoController<Editora> {

    public EditoraController(EditoraService service) {
        super(service, "/api/v1/editoras");
    }
}
