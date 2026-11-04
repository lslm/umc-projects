package dev.lslm.library.controller;

import dev.lslm.library.dto.BibliotecaDtos.BibliotecaRequest;
import dev.lslm.library.dto.BibliotecaDtos.BibliotecaResponse;
import dev.lslm.library.service.BibliotecaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** RF01 - administracao das bibliotecas contratantes (nivel plataforma, sem X-Biblioteca-Id). */
@RestController
@RequestMapping("/api/v1/bibliotecas")
public class BibliotecaController {

    private final BibliotecaService service;

    public BibliotecaController(BibliotecaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BibliotecaResponse> criar(@RequestBody BibliotecaRequest req) {
        BibliotecaResponse r = BibliotecaResponse.from(service.criar(req));
        return ResponseEntity.created(URI.create("/api/v1/bibliotecas/" + r.id())).body(r);
    }

    @GetMapping
    public List<BibliotecaResponse> listar() {
        return service.listar().stream().map(BibliotecaResponse::from).toList();
    }

    @GetMapping("/{id}")
    public BibliotecaResponse buscar(@PathVariable UUID id) {
        return BibliotecaResponse.from(service.buscar(id));
    }

    @PutMapping("/{id}")
    public BibliotecaResponse atualizar(@PathVariable UUID id, @RequestBody BibliotecaRequest req) {
        return BibliotecaResponse.from(service.atualizar(id, req));
    }

    @PostMapping("/{id}/ativacao")
    public BibliotecaResponse ativar(@PathVariable UUID id) {
        return BibliotecaResponse.from(service.alterarAtivacao(id, true));
    }

    @PostMapping("/{id}/desativacao")
    public BibliotecaResponse desativar(@PathVariable UUID id) {
        return BibliotecaResponse.from(service.alterarAtivacao(id, false));
    }
}
