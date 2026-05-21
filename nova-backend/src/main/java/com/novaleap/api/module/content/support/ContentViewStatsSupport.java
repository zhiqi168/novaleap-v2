package com.novaleap.api.module.content.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class ContentViewStatsSupport {

    private static final Duration VIEW_TOTAL_TTL = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;

    public ContentViewStatsSupport(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public int incrementAndGet(String pendingKey, String totalKeyPrefix, String hotKey, Long contentId, int fallbackBaseCount) {
        if (contentId == null || contentId <= 0) {
            return Math.max(0, fallbackBaseCount);
        }
        String totalKey = totalKey(totalKeyPrefix, contentId);
        incrementPending(pendingKey, contentId);
        incrementHot(hotKey, contentId);

        Long next = null;
        try {
            String cachedTotal = redisTemplate.opsForValue().get(totalKey);
            if (!StringUtils.hasText(cachedTotal)) {
                redisTemplate.opsForValue().set(totalKey, String.valueOf(Math.max(0, fallbackBaseCount)), withJitter(VIEW_TOTAL_TTL));
            }
            next = redisTemplate.opsForValue().increment(totalKey, 1L);
            redisTemplate.expire(totalKey, withJitter(VIEW_TOTAL_TTL));
        } catch (Exception ignore) {
            next = null;
        }
        if (next != null) {
            return safeInt(next);
        }
        long pending = readPendingCount(pendingKey, contentId);
        return safeInt(Math.max(0L, fallbackBaseCount) + Math.max(0L, pending));
    }

    public int resolveViewCount(String pendingKey, String totalKeyPrefix, Long contentId, Integer fallbackBaseCount) {
        int base = Math.max(0, fallbackBaseCount == null ? 0 : fallbackBaseCount);
        if (contentId == null || contentId <= 0) {
            return base;
        }
        String totalKey = totalKey(totalKeyPrefix, contentId);
        Long total = null;
        try {
            total = parseLong(redisTemplate.opsForValue().get(totalKey));
        } catch (Exception ignore) {
            total = null;
        }
        if (total != null && total >= 0L) {
            return safeInt(Math.max(total, base));
        }
        long pending = readPendingCount(pendingKey, contentId);
        long resolved = Math.max(0L, base) + Math.max(0L, pending);
        if (resolved > 0L) {
            try {
                redisTemplate.opsForValue().set(totalKey, String.valueOf(resolved), withJitter(VIEW_TOTAL_TTL));
            } catch (Exception ignore) {
                log.debug("Redis write total view count failed for key={}", totalKey, ignore);
            }
        }
        return safeInt(resolved);
    }

    public Map<Long, Integer> resolveViewCountMap(String pendingKey, String totalKeyPrefix, Map<Long, Integer> fallbackBaseCountMap) {
        if (fallbackBaseCountMap == null || fallbackBaseCountMap.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = fallbackBaseCountMap.keySet().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Integer> result = new LinkedHashMap<>();
        Map<Long, Long> pendingMap = readPendingCountMap(pendingKey, ids);
        Map<Long, Long> totalMap = readTotalCountMap(totalKeyPrefix, ids);
        for (Long id : ids) {
            int base = Math.max(0, fallbackBaseCountMap.getOrDefault(id, 0));
            Long total = totalMap.get(id);
            if (total != null && total >= 0L) {
                result.put(id, safeInt(Math.max(total, (long) base)));
                continue;
            }
            long resolved = Math.max(0L, base) + Math.max(0L, pendingMap.getOrDefault(id, 0L));
            if (resolved > 0L) {
                try {
                    redisTemplate.opsForValue().set(totalKey(totalKeyPrefix, id), String.valueOf(resolved), withJitter(VIEW_TOTAL_TTL));
                } catch (Exception ignore) {
                    log.debug("Redis write total view count failed for id={}", id, ignore);
                }
            }
            result.put(id, safeInt(resolved));
        }
        return result;
    }

    public Map<Long, Long> loadPendingViewCounts(String pendingKey, int limit) {
        if (!StringUtils.hasText(pendingKey) || limit <= 0) {
            return Collections.emptyMap();
        }
        try {
            Map<Object, Object> raw = redisTemplate.opsForHash().entries(pendingKey);
            if (raw == null || raw.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<Long, Long> result = new LinkedHashMap<>();
            for (Map.Entry<Object, Object> entry : raw.entrySet()) {
                Long contentId = parseLong(entry.getKey());
                Long delta = parseLong(entry.getValue());
                if (contentId == null || delta == null || delta <= 0L) {
                    continue;
                }
                result.put(contentId, delta);
                if (result.size() >= limit) {
                    break;
                }
            }
            return result;
        } catch (Exception ignore) {
            return Collections.emptyMap();
        }
    }

    public void acknowledgePendingViewCount(String pendingKey, Long contentId, long flushedDelta) {
        if (!StringUtils.hasText(pendingKey) || contentId == null || flushedDelta <= 0L) {
            return;
        }
        try {
            Long remaining = redisTemplate.opsForHash().increment(pendingKey, String.valueOf(contentId), -flushedDelta);
            if (remaining == null || remaining <= 0L) {
                redisTemplate.opsForHash().delete(pendingKey, String.valueOf(contentId));
            }
        } catch (Exception ignore) {
            log.debug("Redis acknowledge pending view count failed for contentId={}", contentId, ignore);
        }
    }

    public void resetTotalViewCount(String totalKeyPrefix, Long contentId, int latestDbValue) {
        if (!StringUtils.hasText(totalKeyPrefix) || contentId == null || contentId <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    totalKey(totalKeyPrefix, contentId),
                    String.valueOf(Math.max(0, latestDbValue)),
                    withJitter(VIEW_TOTAL_TTL)
            );
        } catch (Exception ignore) {
            log.debug("Redis reset total view count failed for contentId={}", contentId, ignore);
        }
    }

    public void evictTotalViewCount(String totalKeyPrefix, Long contentId) {
        if (!StringUtils.hasText(totalKeyPrefix) || contentId == null || contentId <= 0) {
            return;
        }
        try {
            redisTemplate.delete(totalKey(totalKeyPrefix, contentId));
        } catch (Exception ignore) {
            log.debug("Redis evict total view count failed for contentId={}", contentId, ignore);
        }
    }

    public List<Long> readTopHotIds(String hotKey, int limit) {
        if (!StringUtils.hasText(hotKey) || limit <= 0) {
            return Collections.emptyList();
        }
        try {
            Set<String> rawIds = redisTemplate.opsForZSet().reverseRange(hotKey, 0, limit - 1L);
            if (rawIds == null || rawIds.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> result = new ArrayList<>(rawIds.size());
            for (String rawId : rawIds) {
                Long parsed = parseLong(rawId);
                if (parsed != null && parsed > 0L) {
                    result.add(parsed);
                }
            }
            return result;
        } catch (Exception ignore) {
            return Collections.emptyList();
        }
    }

    private void incrementPending(String pendingKey, Long contentId) {
        if (!StringUtils.hasText(pendingKey) || contentId == null || contentId <= 0) {
            return;
        }
        try {
            redisTemplate.opsForHash().increment(pendingKey, String.valueOf(contentId), 1L);
        } catch (Exception ignore) {
            log.debug("Redis increment pending view count failed for contentId={}", contentId, ignore);
        }
    }

    private void incrementHot(String hotKey, Long contentId) {
        if (!StringUtils.hasText(hotKey) || contentId == null || contentId <= 0) {
            return;
        }
        try {
            redisTemplate.opsForZSet().incrementScore(hotKey, String.valueOf(contentId), 1D);
        } catch (Exception ignore) {
            log.debug("Redis increment hot score failed for contentId={}", contentId, ignore);
        }
    }

    private long readPendingCount(String pendingKey, Long contentId) {
        if (!StringUtils.hasText(pendingKey) || contentId == null || contentId <= 0) {
            return 0L;
        }
        try {
            return Math.max(0L, parseLong(redisTemplate.opsForHash().get(pendingKey, String.valueOf(contentId))) == null
                    ? 0L
                    : parseLong(redisTemplate.opsForHash().get(pendingKey, String.valueOf(contentId))));
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private Map<Long, Long> readPendingCountMap(String pendingKey, List<Long> ids) {
        if (!StringUtils.hasText(pendingKey) || ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<Object> fields = ids.stream().map(String::valueOf).map(value -> (Object) value).toList();
            List<Object> values = redisTemplate.opsForHash().multiGet(pendingKey, fields);
            if (values == null || values.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<Long, Long> result = new LinkedHashMap<>();
            for (int i = 0; i < ids.size(); i++) {
                Long value = i < values.size() ? parseLong(values.get(i)) : null;
                if (value != null && value > 0L) {
                    result.put(ids.get(i), value);
                }
            }
            return result;
        } catch (Exception ignore) {
            return Collections.emptyMap();
        }
    }

    private Map<Long, Long> readTotalCountMap(String totalKeyPrefix, List<Long> ids) {
        if (!StringUtils.hasText(totalKeyPrefix) || ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<String> keys = ids.stream().map(id -> totalKey(totalKeyPrefix, id)).toList();
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null || values.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<Long, Long> result = new LinkedHashMap<>();
            for (int i = 0; i < ids.size(); i++) {
                Long value = i < values.size() ? parseLong(values.get(i)) : null;
                if (value != null && value >= 0L) {
                    result.put(ids.get(i), value);
                }
            }
            return result;
        } catch (Exception ignore) {
            return Collections.emptyMap();
        }
    }

    private String totalKey(String totalKeyPrefix, Long contentId) {
        return totalKeyPrefix + contentId;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignore) {
            return null;
        }
    }

    private int safeInt(long value) {
        if (value <= 0L) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private Duration withJitter(Duration ttl) {
        long baseMillis = ttl.toMillis();
        if (baseMillis <= 0L) {
            return ttl;
        }
        long jitterMillis = Math.max(1000L, baseMillis / 5);
        long extraMillis = ThreadLocalRandom.current().nextLong(jitterMillis + 1L);
        return Duration.ofMillis(baseMillis + extraMillis);
    }
}
