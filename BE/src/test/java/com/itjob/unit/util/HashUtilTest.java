package com.itjob.unit.util;

import com.itjob.util.HashUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - HashUtil")
class HashUtilTest {

    @Test
    @DisplayName("returns correct SHA-256 hex for known value")
    void producesKnownSha256() {
        String hex = HashUtil.sha256("hello");
        assertThat(hex)
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    @DisplayName("always produces 64 hex characters")
    void produces64HexChars() {
        String hex = HashUtil.sha256("anything");
        assertThat(hex).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("same input -> same output (deterministic)")
    void deterministic() {
        assertThat(HashUtil.sha256("otp123")).isEqualTo(HashUtil.sha256("otp123"));
    }

    @Test
    @DisplayName("different inputs produce different outputs")
    void differentInputDiffers() {
        assertThat(HashUtil.sha256("otp123")).isNotEqualTo(HashUtil.sha256("otp124"));
    }
}