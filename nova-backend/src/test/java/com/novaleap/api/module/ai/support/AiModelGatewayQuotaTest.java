package com.novaleap.api.module.ai.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaleap.api.module.ai.audit.AiCallAuditService;
import com.novaleap.api.module.ai.config.AiGatewayProperties;
import com.novaleap.api.module.quota.config.AiQuotaProperties;
import com.novaleap.api.module.quota.support.AiQuotaUsageSupport;
import com.novaleap.api.service.AiLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiModelGatewayQuotaTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private AiLimitService aiLimitService;

    @Mock
    private AiQuotaUsageSupport aiQuotaUsageSupport;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AiCallAuditService aiCallAuditService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AiModelGateway aiModelGateway;
    private String dailyKey;
    private String failureKey;
    private String openKey;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AiQuotaProperties quotaProperties = new AiQuotaProperties();
        quotaProperties.setDailyTokenLimit(10_000_000L);
        quotaProperties.setPerModelTokenLimit(5_000_000L);
        quotaProperties.setModelUsageTtlDays(2);

        AiGatewayProperties gatewayProperties = new AiGatewayProperties();
        gatewayProperties.setCircuitFailureThreshold(3);
        gatewayProperties.setCircuitOpenSeconds(90);
        gatewayProperties.setMinResponseTokens(128);

        aiModelGateway = new AiModelGateway(
                chatClientBuilder,
                redisTemplate,
                aiLimitService,
                quotaProperties,
                gatewayProperties,
                aiQuotaUsageSupport,
                objectMapper,
                aiCallAuditService,
                "model-main",
                "model-fallback"
        );
        dailyKey = "nova:ai:usage:model-main:" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        failureKey = "nova:ai:circuit:fail:coach:model-main";
        openKey = "nova:ai:circuit:open:coach:model-main";
    }

    @Test
    void shouldCheckModelQuotaByDailyKey() {
        when(valueOperations.get(dailyKey)).thenReturn("5000000");

        Object exceeded = ReflectionTestUtils.invokeMethod(aiModelGateway, "isModelQuotaExceeded", "model-main");

        verify(valueOperations).get(dailyKey);
        assertEquals(Boolean.TRUE, exceeded);
    }

    @Test
    void shouldRecordModelUsageWithDailyTtl() {
        Usage usage = mock(Usage.class);
        when(usage.getTotalTokens()).thenReturn(120L);

        ReflectionTestUtils.invokeMethod(
                aiModelGateway,
                "recordModelUsage",
                "model-main",
                AiLimitService.AiModule.COACH,
                usage
        );

        verify(valueOperations).increment(dailyKey, 120L);
        verify(redisTemplate).expire(eq(dailyKey), eq(Duration.ofDays(2)));
        verify(aiLimitService).recordTokenUsage(120L);
        verify(aiCallAuditService).recordSuccess("model-main", AiLimitService.AiModule.COACH, 120L);
    }

    @Test
    void shouldMarkQuotaExceededOnCurrentDayKey() {
        ReflectionTestUtils.invokeMethod(aiModelGateway, "markModelQuotaExceeded", "model-main");

        verify(valueOperations).set(dailyKey, "5000000");
        verify(redisTemplate).expire(eq(dailyKey), eq(Duration.ofDays(2)));
    }

    @Test
    void shouldCapRequestedTokensByRemainingBudget() {
        when(valueOperations.get(dailyKey)).thenReturn("4999400");
        when(aiQuotaUsageSupport.currentTokenUsage()).thenReturn(9_999_200L);

        Object result = ReflectionTestUtils.invokeMethod(aiModelGateway, "applyQuotaBudget", "model-main", 1200);

        assertEquals(600, result);
    }

    @Test
    void shouldRejectWhenRemainingBudgetFallsBelowMinimumResponseTokens() {
        when(valueOperations.get(dailyKey)).thenReturn("4999970");
        when(aiQuotaUsageSupport.currentTokenUsage()).thenReturn(9_999_950L);

        assertThrows(
                IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(aiModelGateway, "applyQuotaBudget", "model-main", 1200)
        );
    }

    @Test
    void shouldOpenCircuitAfterRepeatedRetryableFailures() {
        when(valueOperations.increment(failureKey)).thenReturn(3L);

        ReflectionTestUtils.invokeMethod(
                aiModelGateway,
                "recordModelFailure",
                "model-main",
                AiLimitService.AiModule.COACH,
                new RuntimeException("connection reset by peer")
        );

        verify(valueOperations).increment(failureKey);
        verify(redisTemplate).expire(eq(failureKey), eq(Duration.ofSeconds(120)));
        verify(valueOperations).set(eq(openKey), anyString(), eq(Duration.ofSeconds(90)));
        verify(aiCallAuditService).recordFailure(eq("model-main"), eq(AiLimitService.AiModule.COACH), anyString());
    }
}
