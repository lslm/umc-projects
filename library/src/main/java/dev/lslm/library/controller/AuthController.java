package dev.lslm.library.controller;

import dev.lslm.library.dto.AuthDtos.EsqueciSenhaRequest;
import dev.lslm.library.dto.AuthDtos.LoginRequest;
import dev.lslm.library.dto.AuthDtos.RedefinirSenhaRequest;
import dev.lslm.library.dto.AuthDtos.RefreshRequest;
import dev.lslm.library.dto.AuthDtos.TokenResponse;
import dev.lslm.library.dto.AuthDtos.UsuarioResponse;
import dev.lslm.library.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** RF02 - login, sessao e recuperacao de senha. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest req) {
        return service.login(req.email(), req.senha());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody RefreshRequest req) {
        return service.renovar(req.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest req) {
        service.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UsuarioResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.sessaoAtual(authorization);
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<Map<String, String>> esqueciSenha(@RequestBody EsqueciSenhaRequest req) {
        service.esqueciSenha(req.email());
        return ResponseEntity.accepted()
                .body(Map.of("message", "Se o e-mail estiver cadastrado, enviaremos as instruções de recuperação."));
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@RequestBody RedefinirSenhaRequest req) {
        service.redefinirSenha(req.token(), req.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
