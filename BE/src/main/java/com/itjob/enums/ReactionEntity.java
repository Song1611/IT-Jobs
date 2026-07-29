package com.itjob.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReactionEntity {

    POST("post"),
    COMMENT("comment");

    private final String key;

    public static ReactionEntity fromKey(String key) {
        for (ReactionEntity e : values()) {
            if (e.key.equals(key)) {
                return e;
            }
        }
        return null;
    }
}
