package dev.lslm.library.repository;

import dev.lslm.library.domain.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID>, JpaSpecificationExecutor<Auditoria> {
}
