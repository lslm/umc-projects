package dev.lslm.library.repository;

import dev.lslm.library.domain.Biblioteca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BibliotecaRepository extends JpaRepository<Biblioteca, UUID> {

    List<Biblioteca> findByAtivaTrue();
}
