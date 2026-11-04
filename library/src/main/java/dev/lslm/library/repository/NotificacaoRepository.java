package dev.lslm.library.repository;

import dev.lslm.library.domain.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    Optional<Notificacao> findByIdAndBibliotecaId(UUID id, UUID bibliotecaId);

    Page<Notificacao> findByBibliotecaIdAndLeitorIdOrderByCriadaEmDesc(UUID bibliotecaId, UUID leitorId,
                                                                      Pageable pageable);

    long countByBibliotecaIdAndLeitorIdAndLidaFalse(UUID bibliotecaId, UUID leitorId);
}
