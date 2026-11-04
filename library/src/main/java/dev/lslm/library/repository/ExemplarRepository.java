package dev.lslm.library.repository;

import dev.lslm.library.domain.Exemplar;
import dev.lslm.library.domain.Exemplar.Situacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExemplarRepository extends JpaRepository<Exemplar, UUID>, JpaSpecificationExecutor<Exemplar> {

    Optional<Exemplar> findByIdAndBibliotecaId(UUID id, UUID bibliotecaId);

    /** Bloqueio pessimista: evita dois emprestimos/devolucoes simultaneos do mesmo exemplar. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exemplar e where e.id = :id and e.bibliotecaId = :bib")
    Optional<Exemplar> bloquear(@Param("id") UUID id, @Param("bib") UUID bibliotecaId);

    boolean existsByBibliotecaIdAndCodigoBarras(UUID bibliotecaId, String codigoBarras);

    long countByBibliotecaId(UUID bibliotecaId);

    boolean existsByBibliotecaIdAndLivroId(UUID bibliotecaId, UUID livroId);

    boolean existsByBibliotecaIdAndLivroIdAndSituacao(UUID bibliotecaId, UUID livroId, Situacao situacao);

    boolean existsByBibliotecaIdAndLivroIdAndSituacaoIn(UUID bibliotecaId, UUID livroId, Collection<Situacao> situacoes);

    List<Exemplar> findByBibliotecaIdAndLivroId(UUID bibliotecaId, UUID livroId);

    /** Retorna [livroId, total, disponiveis] por obra. */
    @Query("select e.livroId, count(e), sum(case when e.situacao = :disp then 1 else 0 end) "
            + "from Exemplar e where e.bibliotecaId = :bib and e.livroId in :ids group by e.livroId")
    List<Object[]> contagemPorLivro(@Param("bib") UUID bibliotecaId, @Param("ids") Collection<UUID> ids,
                                    @Param("disp") Situacao disponivel);
}
