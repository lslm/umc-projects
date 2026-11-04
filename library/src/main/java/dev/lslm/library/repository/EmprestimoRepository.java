package dev.lslm.library.repository;

import dev.lslm.library.domain.Emprestimo;
import dev.lslm.library.domain.Emprestimo.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, UUID>, JpaSpecificationExecutor<Emprestimo> {

    Optional<Emprestimo> findByIdAndBibliotecaId(UUID id, UUID bibliotecaId);

    long countByBibliotecaIdAndLeitorIdAndStatus(UUID bibliotecaId, UUID leitorId, Status status);

    boolean existsByBibliotecaIdAndLeitorIdAndStatusAndVencimentoEmBefore(UUID bibliotecaId, UUID leitorId,
                                                                          Status status, Instant limite);

    boolean existsByBibliotecaIdAndLeitorIdAndLivroIdAndStatus(UUID bibliotecaId, UUID leitorId, UUID livroId,
                                                              Status status);

    boolean existsByBibliotecaIdAndExemplarId(UUID bibliotecaId, UUID exemplarId);

    List<Emprestimo> findByBibliotecaIdAndStatus(UUID bibliotecaId, Status status);

    List<Emprestimo> findByBibliotecaIdAndLeitorIdOrderByEmprestadoEmDesc(UUID bibliotecaId, UUID leitorId);

    List<Emprestimo> findByBibliotecaIdAndEmprestadoEmGreaterThanEqualAndEmprestadoEmLessThan(
            UUID bibliotecaId, Instant de, Instant ate);

    long countByBibliotecaIdAndStatusAndDevolvidoEmGreaterThanEqualAndDevolvidoEmLessThan(
            UUID bibliotecaId, Status status, Instant de, Instant ate);
}
