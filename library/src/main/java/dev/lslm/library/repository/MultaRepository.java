package dev.lslm.library.repository;

import dev.lslm.library.domain.Multa;
import dev.lslm.library.domain.Multa.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface MultaRepository extends JpaRepository<Multa, UUID>, JpaSpecificationExecutor<Multa> {

    Optional<Multa> findByIdAndBibliotecaId(UUID id, UUID bibliotecaId);

    boolean existsByBibliotecaIdAndLeitorIdAndStatus(UUID bibliotecaId, UUID leitorId, Status status);

    Optional<Multa> findByBibliotecaIdAndEmprestimoId(UUID bibliotecaId, UUID emprestimoId);
}
