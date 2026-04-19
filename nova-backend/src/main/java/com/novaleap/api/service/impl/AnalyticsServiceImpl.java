package com.novaleap.api.service.impl;

import com.novaleap.api.module.analytics.support.AnalyticsGeoInfo;
import com.novaleap.api.module.analytics.support.AnalyticsGeoService;
import com.novaleap.api.module.analytics.support.AnalyticsVisitStore;
import com.novaleap.api.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final String KEY_PREFIX = "nova:analytics:";
    private static final String ACTOR_GUEST = "guest";
    private static final String ACTOR_USER = "user";
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Duration DAILY_METRIC_TTL = Duration.ofDays(90);

    private final StringRedisTemplate redisTemplate;
    private final AnalyticsGeoService analyticsGeoService;
    private final AnalyticsVisitStore analyticsVisitStore;

    public AnalyticsServiceImpl(
            StringRedisTemplate redisTemplate,
            AnalyticsGeoService analyticsGeoService,
            AnalyticsVisitStore analyticsVisitStore
    ) {
        this.redisTemplate = redisTemplate;
        this.analyticsGeoService = analyticsGeoService;
        this.analyticsVisitStore = analyticsVisitStore;
    }

    @Override
    public void trackVisit(String actorType, String uniqueIdentity, String visitorId, String path, String ipAddress, String actorName) {
        try {
            String actor = ACTOR_USER.equalsIgnoreCase(actorType) ? ACTOR_USER : ACTOR_GUEST;
            String day = LocalDate.now().format(DAY_FMT);

            redisTemplate.opsForValue().increment(pvKey(actor, "total"), 1);
            redisTemplate.opsForValue().increment(pvKey(actor, day), 1);
            redisTemplate.expire(pvKey(actor, day), DAILY_METRIC_TTL);

            String uvIdentity = StringUtils.hasText(uniqueIdentity) ? uniqueIdentity : "visitor:" + visitorId;
            redisTemplate.opsForHyperLogLog().add(uvKey(actor, "total"), uvIdentity);
            redisTemplate.opsForHyperLogLog().add(uvKey(actor, day), uvIdentity);
            redisTemplate.expire(uvKey(actor, day), DAILY_METRIC_TTL);

            if (StringUtils.hasText(path)) {
                String pathPvKey = KEY_PREFIX + "path:pv:" + day + ":" + path;
                redisTemplate.opsForValue().increment(pathPvKey, 1);
                redisTemplate.expire(pathPvKey, DAILY_METRIC_TTL);
            }

            AnalyticsGeoInfo geo = analyticsGeoService.resolveGeoInfo(ipAddress);
            analyticsVisitStore.recordGeo(day, actor, ipAddress, path, geo);
            analyticsVisitStore.recordVisitor(actor, uvIdentity, visitorId, path, ipAddress, geo, actorName);
        } catch (Exception e) {
            log.warn("track visit failed: actorType={}, visitorId={}, path={}", actorType, visitorId, path, e);
        }
    }

    @Override
    public Map<String, Object> getVisitStats() {
        String day = LocalDate.now().format(DAY_FMT);

        long guestPv = getLong(pvKey(ACTOR_GUEST, "total"));
        long userPv = getLong(pvKey(ACTOR_USER, "total"));
        long guestUv = getHllSize(uvKey(ACTOR_GUEST, "total"));
        long userUv = getHllSize(uvKey(ACTOR_USER, "total"));

        long guestPvToday = getLong(pvKey(ACTOR_GUEST, day));
        long userPvToday = getLong(pvKey(ACTOR_USER, day));
        long guestUvToday = getHllSize(uvKey(ACTOR_GUEST, day));
        long userUvToday = getHllSize(uvKey(ACTOR_USER, day));

        Map<String, Object> data = new HashMap<>();
        data.put("guestPv", guestPv);
        data.put("guestUv", guestUv);
        data.put("userPv", userPv);
        data.put("userUv", userUv);
        data.put("guestPvToday", guestPvToday);
        data.put("guestUvToday", guestUvToday);
        data.put("userPvToday", userPvToday);
        data.put("userUvToday", userUvToday);
        data.put("totalPv", guestPv + userPv);
        data.put("totalUv", guestUv + userUv);
        data.put("topCities", analyticsVisitStore.topGeoList(AnalyticsVisitStore.GEO_CITY_TOTAL_KEY, 8));
        data.put("topRegions", analyticsVisitStore.topGeoList(AnalyticsVisitStore.GEO_REGION_TOTAL_KEY, 8));
        data.put("topCitiesToday", analyticsVisitStore.topGeoList(AnalyticsVisitStore.GEO_CITY_DAY_PREFIX + day, 8));
        data.put("topRegionsToday", analyticsVisitStore.topGeoList(AnalyticsVisitStore.GEO_REGION_DAY_PREFIX + day, 8));
        data.put("recentGeoVisits", analyticsVisitStore.recentGeoVisits(20));
        return data;
    }

    @Override
    public Map<String, Object> getVisitorRecords(Integer page, Integer size, String keyword, String actorType) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safeSize = Math.min(200, Math.max(size == null ? 20 : size, 1));
        String kw = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        String actorFilter = StringUtils.hasText(actorType) ? actorType.trim().toLowerCase(Locale.ROOT) : "";

        Set<ZSetOperations.TypedTuple<String>> ordered = analyticsVisitStore.loadOrderedVisitors();
        if (ordered == null || ordered.isEmpty()) {
            return pageResult(safePage, safeSize, 0, Collections.emptyList());
        }

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : ordered) {
            String identity = tuple.getValue();
            if (!StringUtils.hasText(identity)) {
                continue;
            }
            Map<Object, Object> raw = analyticsVisitStore.loadVisitorProfile(identity);
            if (raw == null || raw.isEmpty()) {
                continue;
            }

            String actor = String.valueOf(raw.getOrDefault("actorType", ACTOR_GUEST));
            if (StringUtils.hasText(actorFilter) && !actorFilter.equalsIgnoreCase(actor)) {
                continue;
            }

            String displayName = String.valueOf(raw.getOrDefault("displayName", analyticsVisitStore.actorDisplayName(actor, identity)));
            String username = String.valueOf(raw.getOrDefault("username", ""));
            String ip = String.valueOf(raw.getOrDefault("ip", ""));
            String region = String.valueOf(raw.getOrDefault("region", "未知地区"));
            String city = String.valueOf(raw.getOrDefault("city", "未知城市"));
            String lastPath = String.valueOf(raw.getOrDefault("lastPath", "-"));
            String firstSeenAt = String.valueOf(raw.getOrDefault("firstSeenAt", ""));
            String lastSeenAt = String.valueOf(raw.getOrDefault("lastSeenAt", ""));
            long visitCount = parseLong(raw.get("visitCount"));

            if (!matchesKeyword(kw, displayName, username, ip, region, city, identity, lastPath)) {
                continue;
            }

            Map<String, Object> row = new HashMap<>();
            row.put("identity", identity);
            row.put("actorType", actor);
            row.put("actorLabel", ACTOR_USER.equalsIgnoreCase(actor) ? "注册用户" : "游客");
            row.put("displayName", displayName);
            row.put("username", username);
            row.put("ip", ip);
            row.put("maskedIp", analyticsVisitStore.maskIp(ip));
            row.put("region", region);
            row.put("city", city);
            row.put("lastPath", lastPath);
            row.put("firstSeenAt", firstSeenAt);
            row.put("lastSeenAt", lastSeenAt);
            row.put("visitCount", visitCount);
            filtered.add(row);
        }

        int total = filtered.size();
        int start = (safePage - 1) * safeSize;
        if (start >= total) {
            return pageResult(safePage, safeSize, total, Collections.emptyList());
        }
        int end = Math.min(total, start + safeSize);
        return pageResult(safePage, safeSize, total, filtered.subList(start, end));
    }

    private Map<String, Object> pageResult(int page, int size, int total, List<Map<String, Object>> records) {
        Map<String, Object> data = new HashMap<>();
        data.put("page", page);
        data.put("size", size);
        data.put("total", total);
        data.put("records", records);
        return data;
    }

    private boolean matchesKeyword(String keyword, String... fields) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        for (String field : fields) {
            if (field != null && field.toLowerCase(Locale.ROOT).contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String pvKey(String actor, String suffix) {
        return KEY_PREFIX + "pv:" + actor + ":" + suffix;
    }

    private String uvKey(String actor, String suffix) {
        return KEY_PREFIX + "uv:" + actor + ":" + suffix;
    }

    private long getLong(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(value)) {
                return 0L;
            }
            return Long.parseLong(value);
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private long getHllSize(String key) {
        try {
            Long size = redisTemplate.opsForHyperLogLog().size(key);
            return size == null ? 0L : size;
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private long parseLong(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignore) {
            return 0L;
        }
    }
}
