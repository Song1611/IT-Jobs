package com.itjob.unit.util;

import com.itjob.util.SlugUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - SlugUtil")
class SlugUtilTest {

    private static final Pattern SUFFIX_PATTERN = Pattern.compile("^[a-z0-9]{8}$");

    @Test
    @DisplayName("converts to lowercase")
    void lowercases() {
        assertThat(SlugUtil.generateSlug("Java Developer")).startsWith("java-developer-");
    }

    @Test
    @DisplayName("replaces spaces with hyphens")
    void replacesSpacesWithHyphens() {
        assertThat(SlugUtil.generateSlug("Java Developer")).startsWith("java-developer-");
    }

    @Test
    @DisplayName("removes special characters")
    void removesSpecialCharacters() {
        assertThat(SlugUtil.generateSlug("Java (Spring) #Boot!")).startsWith("java-spring-boot-");
    }

    @Test
    @DisplayName("collapses duplicate hyphens")
    void collapsesDuplicateHyphens() {
        assertThat(SlugUtil.generateSlug("Java   Developer")).startsWith("java-developer-");
    }

    @Test
    @DisplayName("always appends 8-char random suffix")
    void alwaysHasRandomSuffix() {
        String slug = SlugUtil.generateSlug("Backend");
        String[] parts = slug.split("-");
        assertThat(parts).hasSize(2);
        assertThat(parts[1]).matches(SUFFIX_PATTERN);
    }

    @Test
    @DisplayName("null name -> only random 8-char suffix")
    void nullNameReturnsOnlySuffix() {
        String slug = SlugUtil.generateSlug(null);
        assertThat(slug).matches(SUFFIX_PATTERN);
    }

    @Test
    @DisplayName("empty name -> only random 8-char suffix")
    void emptyNameReturnsOnlySuffix() {
        String slug = SlugUtil.generateSlug("");
        assertThat(slug).matches(SUFFIX_PATTERN);
    }

    @Test
    @DisplayName("same input -> unique slugs via random suffix")
    void randomSuffixMakesSlugsUnique() {
        String a = SlugUtil.generateSlug("Frontend Engineer");
        String b = SlugUtil.generateSlug("Frontend Engineer");
        assertThat(a).isNotEqualTo(b);
    }
}