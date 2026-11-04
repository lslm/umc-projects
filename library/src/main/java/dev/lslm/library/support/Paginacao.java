package dev.lslm.library.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Paginacao {

    private Paginacao() {
    }

    /** page comeca em 1; per_page entre 1 e 100. */
    public static Pageable de(int page, int perPage, Sort sort) {
        if (page < 1) {
            throw new IllegalArgumentException("page deve ser maior ou igual a 1.");
        }
        if (perPage < 1 || perPage > 100) {
            throw new IllegalArgumentException("per_page deve estar entre 1 e 100.");
        }
        return PageRequest.of(page - 1, perPage, sort);
    }
}
