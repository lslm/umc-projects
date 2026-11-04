package dev.lslm.library.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Paginacao padrao da API: page, per_page, total e total_pages. */
public record PageResponse<T>(
        List<T> data,
        int page,
        @JsonProperty("per_page") int perPage,
        long total,
        @JsonProperty("total_pages") int totalPages
) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public static <T> PageResponse<T> ofList(List<T> data, Page<?> page) {
        return new PageResponse<>(data, page.getNumber() + 1, page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
