package dev.lslm.library.repository;

import dev.lslm.library.domain.Leitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeitorRepository extends JpaRepository<Leitor, UUID>, JpaSpecificationExecutor<Leitor> {

    Optional<Leitor> findByIdAndBibliotecaId(UUID id, UUID bibliotecaId);

    long countByBibliotecaId(UUID bibliotecaId);

    boolean existsByBibliotecaIdAndEmailIgnoreCase(UUID bibliotecaId, String email);

    Optional<Leitor> findByBibliotecaIdAndEmailIgnoreCase(UUID bibliotecaId, String email);

    List<Leitor> findByBibliotecaId(UUID bibliotecaId);
}
