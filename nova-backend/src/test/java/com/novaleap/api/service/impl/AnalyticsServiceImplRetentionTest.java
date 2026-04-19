package com.novaleap.api.service.impl;

import com.novaleap.api.module.analytics.support.AnalyticsGeoInfo;
import com.novaleap.api.module.analytics.support.AnalyticsGeoService;
import com.novaleap.api.module.analytics.support.AnalyticsVisitStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HyperLogLogOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplRetentionTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private AnalyticsGeoService analyticsGeoService;

    @Mock
    private AnalyticsVisitStore analyticsVisitStore;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HyperLogLogOperations<String, String> hyperLogLogOperations;

    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHyperLogLog()).thenReturn(hyperLogLogOperations);
        when(analyticsGeoService.resolveGeoInfo("127.0.0.1")).thenReturn(new AnalyticsGeoInfo("Shanghai", "Shanghai"));
        analyticsService = new AnalyticsServiceImpl(redisTemplate, analyticsGeoService, analyticsVisitStore);
    }

    @Test
    void shouldExpireDailyAnalyticsKeys() {
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        analyticsService.trackVisit("guest", "visitor:abc", "abc", "/api/home", "127.0.0.1", "Guest");

        verify(valueOperations).increment("nova:analytics:pv:guest:" + day, 1);
        verify(redisTemplate).expire("nova:analytics:pv:guest:" + day, Duration.ofDays(90));
        verify(redisTemplate).expire("nova:analytics:uv:guest:" + day, Duration.ofDays(90));
        verify(redisTemplate).expire("nova:analytics:path:pv:" + day + ":/api/home", Duration.ofDays(90));
    }
}
