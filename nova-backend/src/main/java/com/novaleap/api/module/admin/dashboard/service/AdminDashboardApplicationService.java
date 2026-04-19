package com.novaleap.api.module.admin.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novaleap.api.entity.Note;
import com.novaleap.api.entity.Question;
import com.novaleap.api.entity.User;
import com.novaleap.api.entity.Wish;
import com.novaleap.api.mapper.NoteMapper;
import com.novaleap.api.mapper.QuestionMapper;
import com.novaleap.api.mapper.UserMapper;
import com.novaleap.api.mapper.WishMapper;
import com.novaleap.api.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
public class AdminDashboardApplicationService {

    private static final String AI_AUDIT_RECENT_KEY = "nova:ai:audit:recent";

    private final UserMapper userMapper;
    private final NoteMapper noteMapper;
    private final QuestionMapper questionMapper;
    private final WishMapper wishMapper;
    private final AnalyticsService analyticsService;
    private final StringRedisTemplate redisTemplate;

    public AdminDashboardApplicationService(
            UserMapper userMapper,
            NoteMapper noteMapper,
            QuestionMapper questionMapper,
            WishMapper wishMapper,
            AnalyticsService analyticsService,
            StringRedisTemplate redisTemplate
    ) {
        this.userMapper = userMapper;
        this.noteMapper = noteMapper;
        this.questionMapper = questionMapper;
        this.wishMapper = wishMapper;
        this.analyticsService = analyticsService;
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", safeCount(() -> userMapper.selectCount(null)));
        data.put("registeredUserCount", safeCount(() ->
                userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "USER"))
        ));
        data.put("noteCount", safeCount(() -> noteMapper.selectCount(null)));
        data.put("questionCount", safeCount(() -> questionMapper.selectCount(null)));
        data.put("wishCount", safeCount(() -> wishMapper.selectCount(null)));
        data.put("aiCallCount", safeRecentAiCallCount());
        data.putAll(safeVisitStats());
        return data;
    }

    public Map<String, Object> getSystemMonitor() {
        Map<String, Object> data = new HashMap<>();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;

        data.put("jvmUsedMemoryMB", usedMemory / (1024 * 1024));
        data.put("jvmMaxMemoryMB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
        data.put("currentActiveTasks", Thread.activeCount());
        return data;
    }

    public Map<String, Object> getVisitorRecords(Integer page, Integer size, String keyword, String actorType) {
        return analyticsService.getVisitorRecords(page, size, keyword, actorType);
    }

    private long safeCount(Supplier<Long> supplier) {
        try {
            Long count = supplier.get();
            return count == null ? 0L : count;
        } catch (Exception e) {
            log.warn("dashboard count query failed", e);
            return 0L;
        }
    }

    private Map<String, Object> safeVisitStats() {
        try {
            return analyticsService.getVisitStats();
        } catch (Exception e) {
            log.warn("dashboard visit stats query failed", e);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("guestPv", 0L);
            fallback.put("guestUv", 0L);
            fallback.put("userPv", 0L);
            fallback.put("userUv", 0L);
            fallback.put("guestPvToday", 0L);
            fallback.put("guestUvToday", 0L);
            fallback.put("userPvToday", 0L);
            fallback.put("userUvToday", 0L);
            fallback.put("totalPv", 0L);
            fallback.put("totalUv", 0L);
            fallback.put("topCities", Collections.emptyList());
            fallback.put("topRegions", Collections.emptyList());
            fallback.put("topCitiesToday", Collections.emptyList());
            fallback.put("topRegionsToday", Collections.emptyList());
            fallback.put("recentGeoVisits", Collections.emptyList());
            return fallback;
        }
    }

    private long safeRecentAiCallCount() {
        try {
            Long size = redisTemplate.opsForList().size(AI_AUDIT_RECENT_KEY);
            return size == null ? 0L : size;
        } catch (Exception e) {
            log.warn("dashboard ai audit count query failed", e);
            return 0L;
        }
    }
}
