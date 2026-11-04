package dev.lslm.library.controller;

import dev.lslm.library.dto.AuthDtos.PerfilResponse;
import dev.lslm.library.dto.AuthDtos.UsuarioRequest;
import dev.lslm.library.dto.AuthDtos.UsuarioResponse;
import dev.lslm.library.service.AuthService;
import dev.lslm.library.service.UsuarioService;
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

/** RF03 - usuarios e perfis. */
@RestController
@RequestMapping("/api/v1")
public class UsuarioController {

    private final UsuarioService service;
    private final AuthService authService;

    public UsuarioController(UsuarioService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping("/perfis")
    public List<PerfilResponse> perfis() {
        return authService.perfis();
    }

    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioResponse> criar(@RequestBody UsuarioRequest req) {
        UsuarioResponse r = UsuarioResponse.from(service.criar(req));
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + r.id())).body(r);
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> listar() {
        return service.listar().stream().map(UsuarioResponse::from).toList();
    }

    @GetMapping("/usuarios/{id}")
    public UsuarioResponse buscar(@PathVariable UUID id) {
        return UsuarioResponse.from(service.buscar(id));
    }

    @PutMapping("/usuarios/{id}")
    public UsuarioResponse atualizar(@PathVariable UUID id, @RequestBody UsuarioRequest req) {
        return UsuarioResponse.from(service.atualizar(id, req));
    }
}
