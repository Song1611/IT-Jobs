package com.itjob.controller;

import com.itjob.enums.ViewEntity;
import com.itjob.service.ViewCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/debug/redis")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class RedisDebugController {

    private final ViewCountService viewCountService;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheManager cacheManager;

    @PostMapping("/view/increment/{entity}/{id}")
    public String incrementView(@PathVariable String entity, @PathVariable UUID id,
                                @RequestParam(required = false) String viewerId) {
        ViewEntity ve = ViewEntity.valueOf(entity.toUpperCase());
        viewCountService.incrementView(ve, id, viewerId);
        long delta = viewCountService.getPendingViewDelta(ve, id);
        return "Incremented. Pending delta: " + delta;
    }

    @GetMapping("/view/delta/{entity}/{id}")
    public long getDelta(@PathVariable String entity, @PathVariable UUID id) {
        ViewEntity ve = ViewEntity.valueOf(entity.toUpperCase());
        return viewCountService.getPendingViewDelta(ve, id);
    }

    @GetMapping("/view/dirty")
    public Set<String> getDirtyKeys() {
        return stringRedisTemplate.opsForSet().members("dirty:views");
    }

    @GetMapping("/cache/{cacheName}/{key}")
    public Object peekCache(@PathVariable String cacheName, @PathVariable String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) return "Cache not found: " + cacheName;
        Cache.ValueWrapper wrapper = cache.get(key);
        return wrapper == null ? "MISS" : wrapper.get();
    }
}
