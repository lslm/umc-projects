package dev.lslm.library.repository;

import dev.lslm.library.domain.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessaoRepository extends JpaRepository<Sessao, UUID> {

    Optional<Sessao> findByAccessToken(String accessToken);

    Optional<Sessao> findByRefreshToken(String refreshToken);

    List<Sessao> findByUsuarioIdAndRevogadaFalse(UUID usuarioId);
}
