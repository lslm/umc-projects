package dev.lslm.library.repository;

import dev.lslm.library.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Usuario> findByIdAndBibliotecaId(UUID id, UUID bibliotecaId);

    List<Usuario> findByBibliotecaIdOrderByNome(UUID bibliotecaId);
}
