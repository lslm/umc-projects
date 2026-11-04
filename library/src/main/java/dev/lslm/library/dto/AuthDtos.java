package dev.lslm.library.dto;

import dev.lslm.library.domain.Usuario;
import dev.lslm.library.domain.Usuario.Perfil;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(String email, String senha) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record EsqueciSenhaRequest(String email) {
    }

    public record RedefinirSenhaRequest(String token, String novaSenha) {
    }

    public record UsuarioResponse(UUID id, UUID bibliotecaId, String nome, String email, Perfil perfil,
                                  UUID leitorId, boolean ativo, Instant criadoEm) {
        public static UsuarioResponse from(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getBibliotecaId(), u.getNome(), u.getEmail(), u.getPerfil(),
                    u.getLeitorId(), u.isAtivo(), u.getCriadoEm());
        }
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
                                UsuarioResponse usuario) {
    }

    public record PerfilResponse(Perfil perfil, String descricao, List<String> capacidades) {
    }

    public record UsuarioRequest(String nome, String email, String senha, Perfil perfil, UUID leitorId, Boolean ativo) {
    }
}
