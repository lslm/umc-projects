package dev.lslm.library.repository;

import dev.lslm.library.domain.EventoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, String> {
}
