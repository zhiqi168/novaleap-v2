package com.novaleap.api.service.impl;

import com.novaleap.api.mapper.WishCommentMapper;
import com.novaleap.api.mapper.WishLikeMapper;
import com.novaleap.api.module.wish.support.WishQueueConstants;
import com.novaleap.api.service.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishServiceImplQueueTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private LeaderboardService leaderboardService;

    @Mock
    private WishLikeMapper wishLikeMapper;

    @Mock
    private WishCommentMapper wishCommentMapper;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    private WishServiceImpl wishService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        wishService = new WishServiceImpl(redisTemplate, leaderboardService, wishLikeMapper, wishCommentMapper);
    }

    @Test
    void shouldPushWishIntoPendingQueueWhenFirstEnqueue() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(setOperations.add(WishQueueConstants.PENDING_INDEX_SET, "12")).thenReturn(1L);
        when(listOperations.rightPush(WishQueueConstants.PENDING_QUEUE, "12")).thenReturn(1L);

        wishService.enqueuePendingWish(12L);

        verify(setOperations).add(WishQueueConstants.PENDING_INDEX_SET, "12");
        verify(listOperations).rightPush(WishQueueConstants.PENDING_QUEUE, "12");
    }

    @Test
    void shouldSkipDuplicateWishEnqueueWhenAlreadyIndexed() {
        when(setOperations.add(WishQueueConstants.PENDING_INDEX_SET, "12")).thenReturn(0L);

        wishService.enqueuePendingWish(12L);

        verify(setOperations).add(WishQueueConstants.PENDING_INDEX_SET, "12");
        verify(listOperations, never()).rightPush(WishQueueConstants.PENDING_QUEUE, "12");
    }
}
