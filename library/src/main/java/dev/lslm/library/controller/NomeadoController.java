package dev.lslm.library.controller;

import dev.lslm.library.domain.Nomeado;
import dev.lslm.library.dto.AcervoDtos.NomeadoRequest;
import dev.lslm.library.dto.AcervoDtos.NomeadoResponse;
import dev.lslm.library.service.NomeadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Endpoints comuns de autores, editoras e categorias (RF04). */
public abstract class NomeadoController<T extends Nomeado> {

    private final NomeadoService<T> service;
    private final String basePath;

    protected NomeadoController(NomeadoService<T> service, String basePath) {
        this.service = service;
        this.basePath = basePath;
    }

    @PostMapping
    public ResponseEntity<NomeadoResponse> criar(@RequestBody NomeadoRequest req) {
        NomeadoResponse r = NomeadoResponse.from(service.criar(req.nome()));
        return ResponseEntity.created(URI.create(basePath + "/" + r.id())).body(r);
    }

    @GetMapping
    public List<NomeadoResponse> listar() {
        return service.listar().stream().map(NomeadoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public NomeadoResponse buscar(@PathVariable UUID id) {
        return NomeadoResponse.from(service.buscar(id));
    }

    @PutMapping("/{id}")
    public NomeadoResponse atualizar(@PathVariable UUID id, @RequestBody NomeadoRequest req) {
        return NomeadoResponse.from(service.atualizar(id, req.nome()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
