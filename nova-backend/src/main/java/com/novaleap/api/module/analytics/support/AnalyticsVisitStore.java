package com.novaleap.api.module.analytics.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnalyticsVisitStore {

    private static final String KEY_PREFIX = "nova:analytics:";
    public static final String GEO_CITY_TOTAL_KEY = KEY_PREFIX + "geo:city:total";
    public static final String GEO_REGION_TOTAL_KEY = KEY_PREFIX + "geo:region:total";
    public static final String GEO_CITY_DAY_PREFIX = KEY_PREFIX + "geo:city:day:";
    public static final String GEO_REGION_DAY_PREFIX = KEY_PREFIX + "geo:region:day:";
    private static final String GEO_RECENT_LIST_KEY = KEY_PREFIX + "geo:recent";
    private static final int GEO_RECENT_MAX = 300;
    public static final String VISITOR_LAST_SEEN_ZSET_KEY = KEY_PREFIX + "visitor:last-seen";
    private static final String VISITOR_PROFILE_PREFIX = KEY_PREFIX + "visitor:profile:";
    private static final String VISITOR_CLEANUP_LOCK_KEY = KEY_PREFIX + "visitor:cleanup:lock";
    private static final Duration GEO_DAY_TTL = Duration.ofDays(90);
    private static final Duration GEO_RECENT_TTL = Duration.ofDays(14);
    private static final Duration VISITOR_PROFILE_TTL = Duration.ofDays(30);
    private static final Duration VISITOR_RETENTION = Duration.ofDays(30);
    private static final Duration VISITOR_CLEANUP_LOCK_TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AnalyticsVisitStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void recordGeo(String day, String actor, String ipAddress, String path, AnalyticsGeoInfo geo) {
        String region = hasText(geo.region()) ? geo.region() : "未知地区";
        String city = hasText(geo.city()) ? geo.city() : "未知城市";

        redisTemplate.opsForZSet().incrementScore(GEO_REGION_TOTAL_KEY, region, 1D);
        redisTemplate.opsForZSet().incrementScore(GEO_CITY_TOTAL_KEY, city, 1D);
        redisTemplate.opsForZSet().incrementScore(GEO_REGION_DAY_PREFIX + day, region, 1D);
        redisTemplate.opsForZSet().incrementScore(GEO_CITY_DAY_PREFIX + day, city, 1D);
        redisTemplate.expire(GEO_REGION_DAY_PREFIX + day, GEO_DAY_TTL);
        redisTemplate.expire(GEO_CITY_DAY_PREFIX + day, GEO_DAY_TTL);

        Map<String, Object> visit = new LinkedHashMap<>();
        visit.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        visit.put("actorType", actor);
        visit.put("ip", safeIp(ipAddress));
        visit.put("region", region);
        visit.put("city", city);
        visit.put("path", hasText(path) ? path : "-");

        try {
            String raw = objectMapper.writeValueAsString(visit);
            redisTemplate.opsForList().leftPush(GEO_RECENT_LIST_KEY, raw);
            redisTemplate.opsForList().trim(GEO_RECENT_LIST_KEY, 0, GEO_RECENT_MAX - 1);
            redisTemplate.expire(GEO_RECENT_LIST_KEY, GEO_RECENT_TTL);
        } catch (Exception ignore) {
        }
    }

    public void recordVisitor(String actor, String identity, String visitorId, String path, String ipAddress, AnalyticsGeoInfo geo, String actorName) {
        if (!hasText(identity)) {
            return;
        }
        String profileKey = visitorProfileKey(identity);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String ip = safeIp(ipAddress);
        String region = hasText(geo.region()) ? geo.region() : "未知地区";
        String city = hasText(geo.city()) ? geo.city() : "未知城市";
        String name = hasText(actorName) ? actorName.trim() : actorDisplayName(actor, identity);
        String username = "";
        if ("user".equalsIgnoreCase(actor) || identity.startsWith("guest:")) {
            int idx = identity.indexOf(':');
            username = idx > 0 && idx + 1 < identity.length() ? identity.substring(idx + 1) : identity;
        }

        redisTemplate.opsForZSet().add(VISITOR_LAST_SEEN_ZSET_KEY, identity, System.currentTimeMillis());
        redisTemplate.opsForHash().put(profileKey, "identity", identity);
        redisTemplate.opsForHash().put(profileKey, "actorType", actor);
        redisTemplate.opsForHash().put(profileKey, "displayName", name);
        redisTemplate.opsForHash().put(profileKey, "username", username);
        redisTemplate.opsForHash().put(profileKey, "visitorId", hasText(visitorId) ? visitorId : "");
        redisTemplate.opsForHash().put(profileKey, "ip", ip);
        redisTemplate.opsForHash().put(profileKey, "region", region);
        redisTemplate.opsForHash().put(profileKey, "city", city);
        redisTemplate.opsForHash().put(profileKey, "lastPath", hasText(path) ? path : "-");
        redisTemplate.opsForHash().put(profileKey, "lastSeenAt", now);
        redisTemplate.opsForHash().putIfAbsent(profileKey, "firstSeenAt", now);
        redisTemplate.opsForHash().increment(profileKey, "visitCount", 1);
        redisTemplate.expire(profileKey, VISITOR_PROFILE_TTL);
        cleanupExpiredVisitors();
    }

    public Set<ZSetOperations.TypedTuple<String>> loadOrderedVisitors() {
        return redisTemplate.opsForZSet().reverseRangeWithScores(VISITOR_LAST_SEEN_ZSET_KEY, 0, -1);
    }

    public Map<Object, Object> loadVisitorProfile(String identity) {
        return redisTemplate.opsForHash().entries(visitorProfileKey(identity));
    }

    public List<Map<String, Object>> topGeoList(String key, int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, 0, Math.max(0, limit - 1));
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", tuple.getValue());
            row.put("count", tuple.getScore() == null ? 0 : Math.round(tuple.getScore()));
            list.add(row);
        }
        return list;
    }

    public List<Map<String, Object>> recentGeoVisits(int limit) {
        List<String> rows = redisTemplate.opsForList().range(GEO_RECENT_LIST_KEY, 0, Math.max(0, limit - 1));
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String row : rows) {
            if (!hasText(row)) {
                continue;
            }
            try {
                Map<String, Object> parsed = objectMapper.readValue(row, new TypeReference<>() {});
                parsed.put("ip", maskIp(String.valueOf(parsed.getOrDefault("ip", ""))));
                result.add(parsed);
            } catch (Exception ignore) {
            }
        }
        return result;
    }

    public String actorDisplayName(String actor, String identity) {
        if ("user".equalsIgnoreCase(actor)) {
            int idx = identity.indexOf(':');
            return idx > 0 && idx + 1 < identity.length() ? identity.substring(idx + 1) : identity;
        }
        if (identity.startsWith("guest:")) {
            return identity.substring("guest:".length());
        }
        if (identity.startsWith("visitor:")) {
            return "游客" + identity.substring("visitor:".length());
        }
        return "游客";
    }

    public String maskIp(String ip) {
        if (!hasText(ip)) {
            return "-";
        }
        String val = ip.trim();
        if (val.contains(".")) {
            String[] parts = val.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        }
        if (val.contains(":")) {
            int idx = val.lastIndexOf(':');
            if (idx > 0) {
                return val.substring(0, idx) + ":*";
            }
        }
        return val;
    }

    private String visitorProfileKey(String identity) {
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(identity.getBytes(StandardCharsets.UTF_8));
        return VISITOR_PROFILE_PREFIX + encoded;
    }

    private void cleanupExpiredVisitors() {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(VISITOR_CLEANUP_LOCK_KEY, "1", VISITOR_CLEANUP_LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        long cutoff = System.currentTimeMillis() - VISITOR_RETENTION.toMillis();
        Set<String> staleIdentities = redisTemplate.opsForZSet().rangeByScore(VISITOR_LAST_SEEN_ZSET_KEY, 0, cutoff, 0, 200);
        if (staleIdentities == null || staleIdentities.isEmpty()) {
            return;
        }
        for (String identity : staleIdentities) {
            if (!hasText(identity)) {
                continue;
            }
            redisTemplate.opsForZSet().remove(VISITOR_LAST_SEEN_ZSET_KEY, identity);
            redisTemplate.delete(visitorProfileKey(identity));
        }
    }

    private String safeIp(String ipAddress) {
        return hasText(ipAddress) ? ipAddress.trim() : "";
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}
