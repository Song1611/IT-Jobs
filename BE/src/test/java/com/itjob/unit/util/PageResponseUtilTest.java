package com.itjob.unit.util;

import com.itjob.dto.response.PageResponse;
import com.itjob.util.PageResponseUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - PageResponseUtil")
class PageResponseUtilTest {

    private Page<String> pageOf() {
        return new PageImpl<>(List.of(new String[]{"a", "b"}), PageRequest.of(1, 2), 5);
    }

    @Test
    @DisplayName("build with provided items -> preserves items and metadata")
    void buildWithPreMappedItems() {
        Page<String> page = pageOf();
        List<String> items = List.of("A", "B");

        PageResponse<String> result = PageResponseUtil.build(page, items);

        assertThat(result.getItems()).isEqualTo(items);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("build with mapper function -> maps each item")
    void buildWithMapper() {
        Page<String> page = pageOf();

        Function<String, Integer> mapper = String::length;
        PageResponse<Integer> result = PageResponseUtil.build(page, mapper);

        assertThat(result.getItems()).containsExactly(1, 1);
        assertThat(result.getTotalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("empty page -> empty items, metadata preserved")
    void emptyPage() {
        Page<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 2), 0);

        PageResponse<String> result = PageResponseUtil.build(page, List.of());

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }
}