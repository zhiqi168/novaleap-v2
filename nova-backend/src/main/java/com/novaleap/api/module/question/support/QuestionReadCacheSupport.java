package com.novaleap.api.module.question.support;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaleap.api.module.content.support.ContentViewStatsSupport;
import com.novaleap.api.module.question.vo.QuestionAnswerVO;
import com.novaleap.api.module.question.vo.QuestionCategoryOptionVO;
import com.novaleap.api.module.question.vo.QuestionDetailVO;
import com.novaleap.api.module.question.vo.QuestionListItemVO;
import com.novaleap.api.module.questionbank.vo.CustomQuestionBankVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class QuestionReadCacheSupport {

    private static final Duration QUESTION_LIST_TTL = Duration.ofMinutes(2);
    private static final Duration QUESTION_DETAIL_TTL = Duration.ofMinutes(10);
    private static final Duration QUESTION_ANSWER_TTL = Duration.ofMinutes(30);
    private static final Duration QUESTION_CATEGORIES_TTL = Duration.ofMinutes(30);
    private static final Duration RANDOM_POOL_TTL = Duration.ofMinutes(5);
    private static final Duration MY_BANKS_TTL = Duration.ofMinutes(1);
    private static final Duration HOT_KEY_LOCK_TTL = Duration.ofSeconds(3);

    private static final String OFFICIAL_LIST_PREFIX = "nova:question:list:official:";
    private static final String BANK_LIST_PREFIX = "nova:question:list:bank:";
    private static final String OFFICIAL_DETAIL_PREFIX = "nova:question:detail:official:";
    private static final String OFFICIAL_ANSWER_PREFIX = "nova:question:answer:official:";
    private static final String CUSTOM_DETAIL_PREFIX = "nova:question:detail:custom:";
    private static final String CUSTOM_ANSWER_PREFIX = "nova:question:answer:custom:";
    private static final String CATEGORIES_KEY = "nova:question:categories";
    private static final String OFFICIAL_RANDOM_PREFIX = "nova:question:random:official:";
    private static final String BANK_RANDOM_PREFIX = "nova:question:random:bank:";
    private static final String MY_BANKS_PREFIX = "nova:question-bank:mine:";
    private static final String OFFICIAL_DETAIL_LOCK_PREFIX = "nova:lock:question:detail:official:";
    private static final String OFFICIAL_ANSWER_LOCK_PREFIX = "nova:lock:question:answer:official:";
    private static final String VIEW_PENDING_KEY = "nova:question:view:pending";
    private static final String VIEW_TOTAL_PREFIX = "nova:question:view:total:";
    private static final String HOT_RANK_KEY = "nova:question:hot:zset";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ContentViewStatsSupport contentViewStatsSupport;

    public QuestionReadCacheSupport(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ContentViewStatsSupport contentViewStatsSupport
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.contentViewStatsSupport = contentViewStatsSupport;
    }

    public String officialListKey(Integer page, Integer size, String category, Integer difficulty, String keyword) {
        return OFFICIAL_LIST_PREFIX
                + safe(page) + ":"
                + safe(size) + ":"
                + normalize(category) + ":"
                + safe(difficulty) + ":"
                + normalize(keyword);
    }

    public String bankListKey(Long bankId, Integer page, Integer size, String category, Integer difficulty, String keyword) {
        return BANK_LIST_PREFIX
                + safe(bankId) + ":"
                + safe(page) + ":"
                + safe(size) + ":"
                + normalize(category) + ":"
                + safe(difficulty) + ":"
                + normalize(keyword);
    }

    public String officialRandomPoolKey(String category, Integer difficulty) {
        return OFFICIAL_RANDOM_PREFIX + normalize(category) + ":" + safe(difficulty);
    }

    public String bankRandomPoolKey(Long bankId, String category, Integer difficulty) {
        return BANK_RANDOM_PREFIX + safe(bankId) + ":" + normalize(category) + ":" + safe(difficulty);
    }

    public String myBanksKey(Long userId, Integer page, Integer size, Integer status) {
        return MY_BANKS_PREFIX
                + safe(userId) + ":"
                + safe(page) + ":"
                + safe(size) + ":"
                + safe(status);
    }

    public Page<QuestionListItemVO> readQuestionListPage(String key) {
        JavaType type = objectMapper.getTypeFactory()
                .constructParametricType(Page.class, QuestionListItemVO.class);
        return readValue(key, type);
    }

    public void writeQuestionListPage(String key, Page<QuestionListItemVO> value) {
        writeValue(key, value, QUESTION_LIST_TTL);
    }

    public QuestionDetailVO readOfficialQuestionDetail(Long questionId) {
        return readValue(OFFICIAL_DETAIL_PREFIX + safe(questionId), QuestionDetailVO.class);
    }

    public void writeOfficialQuestionDetail(Long questionId, QuestionDetailVO value) {
        writeValue(OFFICIAL_DETAIL_PREFIX + safe(questionId), value, QUESTION_DETAIL_TTL);
    }

    public QuestionAnswerVO readOfficialQuestionAnswer(Long questionId) {
        return readValue(OFFICIAL_ANSWER_PREFIX + safe(questionId), QuestionAnswerVO.class);
    }

    public void writeOfficialQuestionAnswer(Long questionId, QuestionAnswerVO value) {
        writeValue(OFFICIAL_ANSWER_PREFIX + safe(questionId), value, QUESTION_ANSWER_TTL);
    }

    public QuestionDetailVO readScopedCustomQuestionDetail(String scope, Long questionId) {
        return readValue(customDetailKey(scope, questionId), QuestionDetailVO.class);
    }

    public void writeScopedCustomQuestionDetail(String scope, Long questionId, QuestionDetailVO value) {
        writeValue(customDetailKey(scope, questionId), value, QUESTION_DETAIL_TTL);
    }

    public QuestionAnswerVO readScopedCustomQuestionAnswer(String scope, Long questionId) {
        return readValue(customAnswerKey(scope, questionId), QuestionAnswerVO.class);
    }

    public void writeScopedCustomQuestionAnswer(String scope, Long questionId, QuestionAnswerVO value) {
        writeValue(customAnswerKey(scope, questionId), value, QUESTION_ANSWER_TTL);
    }

    public int incrementAndGetOfficialQuestionViewCount(Long questionId, int fallbackBaseCount) {
        return contentViewStatsSupport.incrementAndGet(VIEW_PENDING_KEY, VIEW_TOTAL_PREFIX, HOT_RANK_KEY, questionId, fallbackBaseCount);
    }

    public int resolveOfficialQuestionViewCount(Long questionId, Integer fallbackBaseCount) {
        return contentViewStatsSupport.resolveViewCount(VIEW_PENDING_KEY, VIEW_TOTAL_PREFIX, questionId, fallbackBaseCount);
    }

    public Map<Long, Integer> resolveOfficialQuestionViewCountMap(Map<Long, Integer> fallbackBaseCountMap) {
        return contentViewStatsSupport.resolveViewCountMap(VIEW_PENDING_KEY, VIEW_TOTAL_PREFIX, fallbackBaseCountMap);
    }

    public Map<Long, Long> loadPendingViewCounts(int limit) {
        return contentViewStatsSupport.loadPendingViewCounts(VIEW_PENDING_KEY, limit);
    }

    public void acknowledgePendingViewCount(Long questionId, long flushedDelta) {
        contentViewStatsSupport.acknowledgePendingViewCount(VIEW_PENDING_KEY, questionId, flushedDelta);
    }

    public void resetOfficialQuestionViewCount(Long questionId, Integer latestDbValue) {
        contentViewStatsSupport.resetTotalViewCount(VIEW_TOTAL_PREFIX, questionId, latestDbValue == null ? 0 : latestDbValue);
    }

    public void evictOfficialQuestionViewCount(Long questionId) {
        contentViewStatsSupport.evictTotalViewCount(VIEW_TOTAL_PREFIX, questionId);
    }

    public List<Long> readHotQuestionIds(int limit) {
        return contentViewStatsSupport.readTopHotIds(HOT_RANK_KEY, limit);
    }

    public String tryLockOfficialQuestionDetail(Long questionId) {
        return tryAcquireLock(OFFICIAL_DETAIL_LOCK_PREFIX + safe(questionId));
    }

    public void unlockOfficialQuestionDetail(Long questionId, String token) {
        releaseLock(OFFICIAL_DETAIL_LOCK_PREFIX + safe(questionId), token);
    }

    public String tryLockOfficialQuestionAnswer(Long questionId) {
        return tryAcquireLock(OFFICIAL_ANSWER_LOCK_PREFIX + safe(questionId));
    }

    public void unlockOfficialQuestionAnswer(Long questionId, String token) {
        releaseLock(OFFICIAL_ANSWER_LOCK_PREFIX + safe(questionId), token);
    }

    public List<QuestionCategoryOptionVO> readQuestionCategories() {
        JavaType type = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, QuestionCategoryOptionVO.class);
        return readValue(CATEGORIES_KEY, type);
    }

    public void writeQuestionCategories(List<QuestionCategoryOptionVO> value) {
        writeValue(CATEGORIES_KEY, value == null ? Collections.emptyList() : value, QUESTION_CATEGORIES_TTL);
    }

    public List<Long> readRandomPool(String key) {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class);
        return readValue(key, type);
    }

    public void writeRandomPool(String key, List<Long> value) {
        writeValue(key, value == null ? Collections.emptyList() : value, RANDOM_POOL_TTL);
    }

    public Page<CustomQuestionBankVO> readMyBanksPage(String key) {
        JavaType type = objectMapper.getTypeFactory()
                .constructParametricType(Page.class, CustomQuestionBankVO.class);
        return readValue(key, type);
    }

    public void writeMyBanksPage(String key, Page<CustomQuestionBankVO> value) {
        writeValue(key, value, MY_BANKS_TTL);
    }

    public void evictQuestionLists() {
        deleteByPrefix(OFFICIAL_LIST_PREFIX);
        deleteByPrefix(BANK_LIST_PREFIX);
    }

    public void evictQuestionCategories() {
        redisTemplate.delete(CATEGORIES_KEY);
    }

    public void evictQuestionDetail(Long questionId) {
        redisTemplate.delete(OFFICIAL_DETAIL_PREFIX + safe(questionId));
    }

    public void evictQuestionAnswer(Long questionId) {
        redisTemplate.delete(OFFICIAL_ANSWER_PREFIX + safe(questionId));
    }

    public void evictRandomPools() {
        deleteByPrefix(OFFICIAL_RANDOM_PREFIX);
        deleteByPrefix(BANK_RANDOM_PREFIX);
    }

    public void evictAllMyBanks() {
        deleteByPrefix(MY_BANKS_PREFIX);
    }

    public void evictMyBanks(Long userId) {
        if (userId == null) {
            evictAllMyBanks();
            return;
        }
        deleteByPrefix(MY_BANKS_PREFIX + safe(userId) + ":");
    }

    public void evictAllQuestionReadCaches() {
        evictQuestionLists();
        evictQuestionCategories();
        evictRandomPools();
        deleteByPrefix(OFFICIAL_DETAIL_PREFIX);
        deleteByPrefix(OFFICIAL_ANSWER_PREFIX);
        deleteByPrefix(CUSTOM_DETAIL_PREFIX);
        deleteByPrefix(CUSTOM_ANSWER_PREFIX);
    }

    private <T> T readValue(String key, Class<T> type) {
        try {
            String payload = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(payload)) {
                return null;
            }
            return objectMapper.readValue(payload, type);
        } catch (Exception ignore) {
            redisTemplate.delete(key);
            return null;
        }
    }

    private <T> T readValue(String key, JavaType type) {
        try {
            String payload = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(payload)) {
                return null;
            }
            return objectMapper.readValue(payload, type);
        } catch (Exception ignore) {
            redisTemplate.delete(key);
            return null;
        }
    }

    private void writeValue(String key, Object value, Duration ttl) {
        if (value == null || !StringUtils.hasText(key) || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), withJitter(ttl));
        } catch (Exception ignore) {
            log.debug("Redis write value failed for key={}", key, ignore);
        }
    }

    private String tryAcquireLock(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        try {
            String token = UUID.randomUUID().toString();
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, token, HOT_KEY_LOCK_TTL);
            return Boolean.TRUE.equals(locked) ? token : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private void releaseLock(String key, String token) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(token)) {
            return;
        }
        try {
            String currentToken = redisTemplate.opsForValue().get(key);
            if (token.equals(currentToken)) {
                redisTemplate.delete(key);
            }
        } catch (Exception ignore) {
            log.debug("Redis release lock failed for key={}", key, ignore);
        }
    }

    private void deleteByPrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return;
        }
        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
                org.springframework.data.redis.core.ScanOptions options =
                        org.springframework.data.redis.core.ScanOptions.scanOptions().match(prefix + "*").count(100).build();
                try (var cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        connection.del(cursor.next());
                    }
                }
                return null;
            });
        } catch (Exception ignore) {
            log.debug("Redis delete by prefix failed for prefix={}", prefix, ignore);
        }
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.replace(" ", "_");
    }

    private String customDetailKey(String scope, Long questionId) {
        if (!StringUtils.hasText(scope) || questionId == null) {
            return "";
        }
        return CUSTOM_DETAIL_PREFIX + normalize(scope) + ":" + safe(questionId);
    }

    private String customAnswerKey(String scope, Long questionId) {
        if (!StringUtils.hasText(scope) || questionId == null) {
            return "";
        }
        return CUSTOM_ANSWER_PREFIX + normalize(scope) + ":" + safe(questionId);
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

    private Duration withJitter(Duration ttl) {
        long baseMillis = ttl.toMillis();
        if (baseMillis <= 0) {
            return ttl;
        }
        long jitterMillis = Math.max(1000L, baseMillis / 5);
        long extraMillis = ThreadLocalRandom.current().nextLong(jitterMillis + 1);
        return Duration.ofMillis(baseMillis + extraMillis);
    }

    private String safe(Object value) {
        return Objects.toString(value, "");
    }
}
