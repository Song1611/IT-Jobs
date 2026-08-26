package com.itjob.unit.util;

import com.itjob.util.RedisOperation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit - RedisOperation")
class RedisOperationTest {

    @Nested
    @DisplayName("run")
    class Run {

        @Test
        @DisplayName("executes the action normally")
        void executesAction() {
            boolean[] executed = {false};

            assertThatCode(() -> RedisOperation.run(() -> executed[0] = true, "warn"))
                    .doesNotThrowAnyException();

            assertThat(executed[0]).isTrue();
        }

        @Test
        @DisplayName("swallows RedisConnectionFailureException")
        void swallowsConnectionFailure() {
            assertThatCode(() -> RedisOperation.run(
                    () -> { throw new RedisConnectionFailureException("connection refused"); },
                    "cache unavailable"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("swallows any DataAccessException")
        void swallowsDataAccessException() {
            assertThatCode(() -> RedisOperation.run(
                    () -> { throw new InvalidDataAccessExceptionStub("serialization failed"); },
                    "cache operation failed"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("propagates unrelated exceptions")
        void propagatesOtherExceptions() {
            assertThatThrownBy(() -> RedisOperation.run(
                    () -> { throw new IllegalStateException("bug in caller code"); },
                    "warn"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("supply")
    class Supply {

        @Test
        @DisplayName("returns the produced value on success")
        void returnsValueOnSuccess() {
            String result = RedisOperation.supply(() -> "cached-value", "warn");

            assertThat(result).isEqualTo("cached-value");
        }

        @Test
        @DisplayName("returns null when Redis connection fails")
        void returnsNullOnConnectionFailure() {
            String result = RedisOperation.supply(
                    () -> { throw new RedisConnectionFailureException("connection refused"); },
                    "cache unavailable");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("returns null on any DataAccessException")
        void returnsNullOnDataAccessException() {
            String result = RedisOperation.supply(
                    () -> { throw new InvalidDataAccessExceptionStub("serialization failed"); },
                    "cache operation failed");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("parseUuids from List")
    class ParseUuidsFromList {

        @Test
        @DisplayName("null input -> empty list")
        void nullInputReturnsEmptyList() {
            assertThat(RedisOperation.parseUuids((List<String>) null)).isEmpty();
        }

        @Test
        @DisplayName("empty input -> empty list")
        void emptyInputReturnsEmptyList() {
            assertThat(RedisOperation.parseUuids(List.of())).isEmpty();
        }

        @Test
        @DisplayName("parses all valid uuid strings")
        void parsesValidUuids() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            List<UUID> result = RedisOperation.parseUuids(List.of(id1.toString(), id2.toString()));

            assertThat(result).containsExactly(id1, id2);
        }

        @Test
        @DisplayName("skips invalid entries and keeps valid ones")
        void skipsInvalidEntries() {
            UUID valid = UUID.randomUUID();

            List<UUID> result = RedisOperation.parseUuids(
                    List.of(valid.toString(), "not-a-uuid", ""));

            assertThat(result).containsExactly(valid);
        }
    }

    @Nested
    @DisplayName("parseUuids from Set")
    class ParseUuidsFromSet {

        @Test
        @DisplayName("null set -> empty list")
        void nullSetReturnsEmptyList() {
            assertThat(RedisOperation.parseUuids((Set<String>) null)).isEmpty();
        }

        @Test
        @DisplayName("parses valid uuids and skips invalid ones")
        void parsesValidAndSkipsInvalid() {
            UUID valid = UUID.randomUUID();

            List<UUID> result = RedisOperation.parseUuids(Set.of(valid.toString(), "bad"));

            assertThat(result).containsExactly(valid);
        }
    }

    private static final class InvalidDataAccessExceptionStub extends DataAccessException {
        private InvalidDataAccessExceptionStub(String msg) {
            super(msg);
        }
    }
}