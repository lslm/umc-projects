package dev.lslm.library.repository;

import dev.lslm.library.domain.Reserva;
import dev.lslm.library.domain.Reserva.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, UUID>, JpaSpecificationExecutor<Reserva> {

    Optional<Reserva> findByIdAndBibliotecaId(UUID id, UUID bibliotecaId);

    Optional<Reserva> findFirstByBibliotecaIdAndLivroIdAndStatusOrderByCriadaEmAsc(UUID bibliotecaId, UUID livroId,
                                                                                   Status status);

    long countByBibliotecaIdAndLivroIdAndStatusAndCriadaEmBefore(UUID bibliotecaId, UUID livroId, Status status,
                                                                Instant criadaEm);

    boolean existsByBibliotecaIdAndLeitorIdAndLivroIdAndStatusIn(UUID bibliotecaId, UUID leitorId, UUID livroId,
                                                                Collection<Status> status);

    boolean existsByBibliotecaIdAndLivroIdAndStatus(UUID bibliotecaId, UUID livroId, Status status);

    Optional<Reserva> findFirstByBibliotecaIdAndExemplarIdAndStatus(UUID bibliotecaId, UUID exemplarId, Status status);

    List<Reserva> findByBibliotecaIdAndLeitorIdAndStatusInOrderByCriadaEmAsc(UUID bibliotecaId, UUID leitorId,
                                                                             Collection<Status> status);

    List<Reserva> findByBibliotecaIdAndStatusAndDisponivelAteBefore(UUID bibliotecaId, Status status, Instant limite);
}
