    package com.itjob.service.impl;

    import com.itjob.service.DistributedLockService;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.data.redis.core.StringRedisTemplate;
    import org.springframework.data.redis.core.script.DefaultRedisScript;
    import org.springframework.stereotype.Service;

    import java.util.List;
    import java.util.concurrent.TimeUnit;

    @Service
    @RequiredArgsConstructor
    @Slf4j
    public class DistributedLockServiceImpl implements DistributedLockService {

        private final StringRedisTemplate stringRedisTemplate;

        private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>();

        static {
            UNLOCK_SCRIPT.setScriptText(
                    "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('DEL', KEYS[1]) " +
                    "else " +
                    "  return 0 " +
                    "end"
            );
            UNLOCK_SCRIPT.setResultType(Long.class);
        }

        @Override
        public boolean tryLock(String key, String value, long leaseTime, TimeUnit unit) {
            try {
                Boolean result = stringRedisTemplate.opsForValue()
                        .setIfAbsent(key, value, leaseTime, unit);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                log.warn("Failed to acquire lock for key {}: {}", key, e.getMessage());
                return false;
            }
        }

        @Override
        public void unlock(String key, String value) {
            try {
                Long deleted = stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(key), value);
                if (Long.valueOf(0).equals(deleted)) {
                    log.warn("Unlock attempted on key {} but lock already expired or owned by another holder", key);
                }
            } catch (Exception e) {
                log.warn("Failed to release lock for key {}: {}", key, e.getMessage());
            }
        }
    }
