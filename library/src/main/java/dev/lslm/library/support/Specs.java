package dev.lslm.library.support;

import org.springframework.data.jpa.domain.Specification;

public final class Specs {

    private Specs() {
    }

    /** Filtro de igualdade; valor nulo significa "sem filtro". */
    public static <T> Specification<T> eq(String campo, Object valor) {
        return (root, query, cb) -> valor == null ? cb.conjunction() : cb.equal(root.get(campo), valor);
    }
}
