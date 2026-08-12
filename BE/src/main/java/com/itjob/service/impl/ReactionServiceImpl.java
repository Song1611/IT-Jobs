package com.itjob.service.impl;

import com.itjob.dto.response.ReactionResponse;
import com.itjob.entity.Comment;
import com.itjob.entity.CommentReaction;
import com.itjob.entity.Post;
import com.itjob.entity.Reaction;
import com.itjob.entity.User;
import com.itjob.enums.ReactionEntity;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.*;
import com.itjob.redis.RedisKeys;
import com.itjob.service.ReactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;


@Service
@RequiredArgsConstructor
@Slf4j
public class ReactionServiceImpl implements ReactionService {

    private final StringRedisTemplate stringRedisTemplate;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final UserRepository userRepository;

    private record ToggleResult(long delta, String resultType, boolean reacted) {}

    @Override
    @Transactional
    public ReactionResponse togglePostReaction(UUID postId, UUID userId, String reactionType) {
        String normalizedType = normalizeType(reactionType);
        ToggleResult result = handleToggle(
                normalizedType,
                reactionRepository.findByPostIdAndUserId(postId, userId),
                Reaction::getReactionType,
                reactionRepository::delete,
                r -> { r.setReactionType(normalizedType); reactionRepository.save(r); },
                () -> {
                    Post post = postRepository.findById(postId)
                            .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                    reactionRepository.save(Reaction.builder()
                            .post(post).user(user).reactionType(normalizedType).build());
                });

        // Redis counter is updated only AFTER the DB transaction commits, so a
        // rollback cannot leave a phantom pending delta. Pattern matches the
        // cache eviction in PostServiceImpl.
        runAfterCommit(() -> updateRedisCounter(ReactionEntity.POST, postId, result.delta()));
        long totalCount = getCurrentCount(ReactionEntity.POST, postId, result.delta());
        return ReactionResponse.builder()
                .entityType("post")
                .entityId(postId.toString())
                .reactionType(result.resultType())
                .reactionCount(totalCount)
                .reacted(result.reacted())
                .build();
    }

    @Override
    @Transactional
    public ReactionResponse toggleCommentReaction(UUID commentId, UUID userId, String reactionType) {
        String normalizedType = normalizeType(reactionType);
        ToggleResult result = handleToggle(
                normalizedType,
                commentReactionRepository.findByCommentIdAndUserId(commentId, userId),
                CommentReaction::getReactionType,
                commentReactionRepository::delete,
                r -> { r.setReactionType(normalizedType); commentReactionRepository.save(r); },
                () -> {
                    Comment comment = commentRepository.findById(commentId)
                            .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                    commentReactionRepository.save(CommentReaction.builder()
                            .comment(comment).user(user).reactionType(normalizedType).build());
                });

        runAfterCommit(() -> updateRedisCounter(ReactionEntity.COMMENT, commentId, result.delta()));
        long totalCount = getCurrentCount(ReactionEntity.COMMENT, commentId, result.delta());
        return ReactionResponse.builder()
                .entityType("comment")
                .entityId(commentId.toString())
                .reactionType(result.resultType())
                .reactionCount(totalCount)
                .reacted(result.reacted())
                .build();
    }

    private String normalizeType(String reactionType) {
        String normalized = reactionType != null ? reactionType.toLowerCase() : null;
        if (normalized == null || normalized.isBlank()) {
            throw new AppException(ErrorCode.REACTION_TYPE_REQUIRED);
        }
        return normalized;
    }

    private <T> ToggleResult handleToggle(
            String normalizedType,
            Optional<T> existing,
            Function<T, String> typeGetter,
            Consumer<T> deleter,
            Consumer<T> typeUpdater,
            Runnable creator) {

        if (existing.isPresent()) {
            T reaction = existing.get();
            if (typeGetter.apply(reaction).equals(normalizedType)) {
                deleter.accept(reaction);
                return new ToggleResult(-1L, null, false);
            }
            typeUpdater.accept(reaction);
            return new ToggleResult(0L, normalizedType, true);
        }
        creator.run();
        return new ToggleResult(1L, normalizedType, true);
    }

    @Override
    public long getPendingReactionDelta(ReactionEntity entity, UUID id) {
        try {
            String value = stringRedisTemplate.opsForValue().get(RedisKeys.reactionKey(entity.getKey(), id));
            return value == null ? 0L : Long.parseLong(value);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed for pending delta {}/{}: {}", entity.getKey(), id, e.getMessage());
            return 0L;
        }
    }

    private void updateRedisCounter(ReactionEntity entity, UUID id, long delta) {
        if (delta == 0) return;
        String redisKey = RedisKeys.reactionKey(entity.getKey(), id);
        try {
            stringRedisTemplate.opsForValue().increment(redisKey, delta);
            stringRedisTemplate.opsForSet().add(RedisKeys.DIRTY_REACTION_SET, redisKey);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed for counter {}/{}, falling back to direct DB update: {}", entity.getKey(), id, e.getMessage());
            try {
                getDbUpdater(entity).apply(id, delta);
            } catch (DataAccessException dbEx) {
                log.error("DB fallback also failed for {}/{}: {}", entity.getKey(), id, dbEx.getMessage());
            }
        }
    }

    /**
     * Current count = DB synced count + pending delta (Redis) + local toggle delta.
     * {@code localDelta} accounts for the in-flight toggle whose Redis increment
     * is deferred to afterCommit, so the API response is accurate before commit.
     */
    private long getCurrentCount(ReactionEntity entity, UUID id, long localDelta) {
        long dbCount = switch (entity) {
            case POST -> postRepository.getReactionCountById(id).orElse(0);
            case COMMENT -> commentRepository.getReactionCountById(id).orElse(0);
        };
        long pendingDelta = getPendingReactionDelta(entity, id);
        return dbCount + pendingDelta + localDelta;
    }

    /** Run a side effect only after the DB transaction commits; else run immediately. */
    private void runAfterCommit(Runnable callback) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    callback.run();
                }
            });
        } else {
            callback.run();
        }
    }

    @Scheduled(fixedDelayString = "${reaction.sync.interval:300000}")
    public void syncToDatabase() {
        Set<String> dirtyKeys;
        try {
            dirtyKeys = stringRedisTemplate.opsForSet().members(RedisKeys.DIRTY_REACTION_SET);
            if (dirtyKeys == null || dirtyKeys.isEmpty()) {
                return;
            }
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed reading dirty set: {}", e.getMessage());
            return;
        }

        for (String redisKey : dirtyKeys) {
            syncKey(redisKey);
        }
    }

    private void syncKey(String redisKey) {
        String deltaStr = stringRedisTemplate.opsForValue().getAndSet(redisKey, "0");
        if (deltaStr == null || deltaStr.equals("0")) {
            stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_REACTION_SET, redisKey);
            return;
        }

        long delta;
        try {
            delta = Long.parseLong(deltaStr);
        } catch (NumberFormatException e) {
            log.warn("Invalid delta value in Redis key {}: {}", redisKey, deltaStr);
            stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_REACTION_SET, redisKey);
            return;
        }

        String[] parts = redisKey.split(":");
        if (parts.length < 3) {
            log.warn("Malformed Redis key: {}", redisKey);
            stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_REACTION_SET, redisKey);
            stringRedisTemplate.unlink(redisKey);
            return;
        }

        UUID id;
        try {
            id = UUID.fromString(parts[2]);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID in key: {}", redisKey);
            stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_REACTION_SET, redisKey);
            stringRedisTemplate.unlink(redisKey);
            return;
        }

        ReactionEntity entity = ReactionEntity.fromKey(parts[1]);
        if (entity == null) {
            log.warn("Unknown entity in key: {}", redisKey);
            stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_REACTION_SET, redisKey);
            stringRedisTemplate.unlink(redisKey);
            return;
        }

        BiFunction<UUID, Long, Integer> updater = getDbUpdater(entity);
        try {
            int affected = updater.apply(id, delta);
            if (affected == 0) {
                log.warn("No {} found for id {}, deleting stale key", entity.getKey(), id);
                stringRedisTemplate.unlink(redisKey);
            }
        } catch (DataAccessException e) {
            log.warn("DB update failed for {} {}, restoring delta: {}", entity.getKey(), id, e.getMessage());
            stringRedisTemplate.opsForValue().increment(redisKey, delta);
            return;
        }

        String remaining = stringRedisTemplate.opsForValue().get(redisKey);
        if (remaining == null || remaining.equals("0")) {
            stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_REACTION_SET, redisKey);
        }
    }

    private BiFunction<UUID, Long, Integer> getDbUpdater(ReactionEntity entity) {
        return switch (entity) {
            case POST -> postRepository::incrementReactionCount;
            case COMMENT -> commentRepository::incrementReactionCount;
        };
    }
}
