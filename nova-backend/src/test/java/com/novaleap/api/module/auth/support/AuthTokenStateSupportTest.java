package com.novaleap.api.module.auth.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenStateSupportTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthTokenStateSupport authTokenStateSupport;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        authTokenStateSupport = new AuthTokenStateSupport(redisTemplate, 86_400_000L);
    }

    @Test
    void shouldKeepNewTokenActiveWhenLogoutAndReloginHappenInSameSecond() {
        when(valueOperations.get("nova:auth:token:invalid-after:user@example.com"))
                .thenReturn("1713519900123");

        boolean oldTokenActive = authTokenStateSupport.isTokenActive(
                "user@example.com",
                new Date(1713519900000L),
                1713519900000L
        );
        boolean newTokenActive = authTokenStateSupport.isTokenActive(
                "user@example.com",
                new Date(1713519900000L),
                1713519900456L
        );

        assertFalse(oldTokenActive);
        assertTrue(newTokenActive);
    }
}
