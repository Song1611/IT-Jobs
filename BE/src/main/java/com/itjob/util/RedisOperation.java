package com.itjob.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class RedisOperation {

    private static final Logger log = LoggerFactory.getLogger(RedisOperation.class);

    private RedisOperation() {}

    public static void run(Runnable action, String warnMsg, Object... args) {
        supply(() -> { action.run(); return null; }, warnMsg, args);
    }

    public static <T> T supply(Supplier<T> action, String warnMsg, Object... args) {
        try {
            return action.get();
        } catch (RedisConnectionFailureException e) {
            log.warn(warnMsg, args);
        } catch (DataAccessException e) {
            log.warn(warnMsg, args);
        }
        return null;
    }

    public static List<UUID> parseUuids(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return List.of();
        }
        return strings.stream()
                .map(id -> { try { return UUID.fromString(id); }
                catch (IllegalArgumentException e) { return null; } })
                .filter(Objects::nonNull)
                .toList();
    }

    public static List<UUID> parseUuids(Set<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return List.of();
        }
        return parseUuids(strings.stream().toList());
    }
}
