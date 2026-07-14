package com.itjob.util;

import com.itjob.dto.response.PageResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Utility class for building PageResponse objects from Spring Data Page.
 * Eliminates duplicate PageResponse.builder() code across services.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PageResponseUtil {
    
    /**
     * Build PageResponse from Spring Data Page with pre-mapped items.
     * <p>Use this when you've already mapped entities to DTOs.
     *
     * @param page Spring Data Page object
     * @param items Pre-mapped list of response DTOs
     * @param <T> Response DTO type
     * @param <E> Entity type
     * @return PageResponse with pagination metadata
     */
    public static <T, E> PageResponse<T> build(Page<E> page, List<T> items) {
        return PageResponse.<T>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
    
    /**
     * Build PageResponse from Spring Data Page with mapper function.
     * <p>Use this for simple mapping where you just need to apply a mapper.
     *
     * @param page Spring Data Page object
     * @param mapper Function to map entity to response DTO
     * @param <T> Response DTO type
     * @param <E> Entity type
     * @return PageResponse with pagination metadata
     */
    public static <T, E> PageResponse<T> build(Page<E> page, Function<E, T> mapper) {
        List<T> items = page.getContent().stream()
                .map(mapper)
                .toList();
        
        return build(page, items);
    }
}
