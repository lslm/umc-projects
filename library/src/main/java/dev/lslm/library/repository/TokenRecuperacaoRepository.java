package dev.lslm.library.repository;

import dev.lslm.library.domain.TokenRecuperacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenRecuperacaoRepository extends JpaRepository<TokenRecuperacao, UUID> {

    Optional<TokenRecuperacao> findByToken(String token);
}
