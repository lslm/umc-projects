package dev.lslm.library.repository;

import dev.lslm.library.domain.Nomeado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface NomeadoRepository<T extends Nomeado> extends JpaRepository<T, UUID> {

    List<T> findByBibliotecaIdOrderByNome(UUID bibliotecaId);

    Optional<T> findByIdAndBibliotecaId(UUID id, UUID bibliotecaId);

    boolean existsByBibliotecaIdAndNomeIgnoreCase(UUID bibliotecaId, String nome);
}
