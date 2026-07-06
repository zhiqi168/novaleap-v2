package com.novaleap.api.module.note.support;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaleap.api.dto.NoteCommentVO;
import com.novaleap.api.module.content.support.ContentViewStatsSupport;
import com.novaleap.api.module.note.vo.NoteDetailVO;
import com.novaleap.api.module.note.vo.NoteListItemVO;
import com.novaleap.api.module.system.security.ActorIdentity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class NoteReadCacheSupport {

    private static final Duration PUBLIC_LIST_TTL = Duration.ofMinutes(2);
    private static final Duration MINE_LIST_TTL = Duration.ofMinutes(1);
    private static final Duration DETAIL_TTL = Duration.ofMinutes(10);
    private static final Duration COMMENTS_TTL = Duration.ofMinutes(1);
    private static final Duration LIKE_STATE_TTL = Duration.ofMinutes(10);
    private static final Duration HOT_KEY_LOCK_TTL = Duration.ofSeconds(3);

    private static final String PUBLIC_LIST_PREFIX = "nova:note:list:public:";
    private static final String MINE_LIST_PREFIX = "nova:note:list:mine:";
    private static final String DETAIL_PREFIX = "nova:note:detail:";
    private static final String COMMENTS_PREFIX = "nova:note:comments:";
    private static final String LIKE_STATE_PREFIX = "nova:note:liked:";
    private static final String DETAIL_LOCK_PREFIX = "nova:lock:note:detail:";
    private static final String VIEW_PENDING_KEY = "nova:note:view:pending";
    private static final String VIEW_TOTAL_PREFIX = "nova:note:view:total:";
    private static final String HOT_RANK_KEY = "nova:note:hot:zset";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ContentViewStatsSupport contentViewStatsSupport;

    public NoteReadCacheSupport(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ContentViewStatsSupport contentViewStatsSupport
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.contentViewStatsSupport = contentViewStatsSupport;
    }

    public String publicListKey(Integer page, Integer size, String keyword, String category) {
        return PUBLIC_LIST_PREFIX
                + safe(page) + ":"
                + safe(size) + ":"
                + normalize(keyword) + ":"
                + normalize(category);
    }

    public String mineListKey(Long userId, Integer page, Integer size, Integer status) {
        return MINE_LIST_PREFIX
                + safe(userId) + ":"
                + safe(page) + ":"
                + safe(size) + ":"
                + safe(status);
    }

    public String detailKey(Long noteId) {
        return DETAIL_PREFIX + safe(noteId);
    }

    public String commentsKey(Long noteId) {
        return COMMENTS_PREFIX + safe(noteId);
    }

    public String likedStateKey(ActorIdentity actor, Long noteId) {
        if (actor == null || actor.isEmpty() || noteId == null) {
            return "";
        }
        return LIKE_STATE_PREFIX + normalize(actor.type()) + ":" + normalize(actor.id()) + ":" + safe(noteId);
    }

    public Page<NoteListItemVO> readNoteListPage(String key) {
        JavaType type = objectMapper.getTypeFactory()
                .constructParametricType(Page.class, NoteListItemVO.class);
        return readValue(key, type);
    }

    public void writePublicListPage(String key, Page<NoteListItemVO> value) {
        writeValue(key, value, PUBLIC_LIST_TTL);
    }

    public void writeMineListPage(String key, Page<NoteListItemVO> value) {
        writeValue(key, value, MINE_LIST_TTL);
    }

    public NoteDetailVO readNoteDetail(Long noteId) {
        return readValue(detailKey(noteId), NoteDetailVO.class);
    }

    public void writeNoteDetail(Long noteId, NoteDetailVO value) {
        writeValue(detailKey(noteId), value, DETAIL_TTL);
    }

    public List<NoteCommentVO> readComments(Long noteId) {
        JavaType type = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, NoteCommentVO.class);
        List<NoteCommentVO> cached = readValue(commentsKey(noteId), type);
        return cached == null ? null : cached;
    }

    public void writeComments(Long noteId, List<NoteCommentVO> value) {
        writeValue(commentsKey(noteId), value == null ? Collections.emptyList() : value, COMMENTS_TTL);
    }

    public Map<Long, Boolean> readLikedStateMap(ActorIdentity actor, List<Long> noteIds) {
        if (actor == null || actor.isEmpty() || noteIds == null || noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> keys = noteIds.stream()
                .filter(Objects::nonNull)
                .map(noteId -> likedStateKey(actor, noteId))
                .filter(StringUtils::hasText)
                .toList();
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null || values.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<Long, Boolean> result = new LinkedHashMap<>();
            for (int i = 0; i < keys.size(); i++) {
                String value = values.get(i);
                if (!StringUtils.hasText(value)) {
                    continue;
                }
                Long noteId = extractNoteId(keys.get(i));
                if (noteId == null) {
                    continue;
                }
                result.put(noteId, Boolean.parseBoolean(value));
            }
            return result;
        } catch (Exception ignore) {
            return Collections.emptyMap();
        }
    }

    public void writeLikedState(ActorIdentity actor, Long noteId, boolean liked) {
        String key = likedStateKey(actor, noteId);
        if (!StringUtils.hasText(key)) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(liked), withJitter(LIKE_STATE_TTL));
        } catch (Exception ignore) {
            log.debug("Redis write liked state failed for noteId={}", noteId, ignore);
        }
    }

    public void writeLikedStates(ActorIdentity actor, List<Long> noteIds, Set<Long> likedNoteIds) {
        if (actor == null || actor.isEmpty() || noteIds == null || noteIds.isEmpty()) {
            return;
        }
        Set<Long> likedSet = likedNoteIds == null ? Set.of() : likedNoteIds;
        for (Long noteId : noteIds) {
            if (noteId == null) {
                continue;
            }
            writeLikedState(actor, noteId, likedSet.contains(noteId));
        }
    }

    public int incrementAndGetNoteViewCount(Long noteId, int fallbackBaseCount) {
        return contentViewStatsSupport.incrementAndGet(VIEW_PENDING_KEY, VIEW_TOTAL_PREFIX, HOT_RANK_KEY, noteId, fallbackBaseCount);
    }

    public int resolveNoteViewCount(Long noteId, Integer fallbackBaseCount) {
        return contentViewStatsSupport.resolveViewCount(VIEW_PENDING_KEY, VIEW_TOTAL_PREFIX, noteId, fallbackBaseCount);
    }

    public Map<Long, Integer> resolveNoteViewCountMap(Map<Long, Integer> fallbackBaseCountMap) {
        return contentViewStatsSupport.resolveViewCountMap(VIEW_PENDING_KEY, VIEW_TOTAL_PREFIX, fallbackBaseCountMap);
    }

    public Map<Long, Long> loadPendingViewCounts(int limit) {
        return contentViewStatsSupport.loadPendingViewCounts(VIEW_PENDING_KEY, limit);
    }

    public void acknowledgePendingViewCount(Long noteId, long flushedDelta) {
        contentViewStatsSupport.acknowledgePendingViewCount(VIEW_PENDING_KEY, noteId, flushedDelta);
    }

    public void resetNoteViewCount(Long noteId, Integer latestDbValue) {
        contentViewStatsSupport.resetTotalViewCount(VIEW_TOTAL_PREFIX, noteId, latestDbValue == null ? 0 : latestDbValue);
    }

    public void evictNoteViewCount(Long noteId) {
        contentViewStatsSupport.evictTotalViewCount(VIEW_TOTAL_PREFIX, noteId);
    }

    public List<Long> readHotNoteIds(int limit) {
        return contentViewStatsSupport.readTopHotIds(HOT_RANK_KEY, limit);
    }

    public String tryLockNoteDetail(Long noteId) {
        return tryAcquireLock(DETAIL_LOCK_PREFIX + safe(noteId));
    }

    public void unlockNoteDetail(Long noteId, String token) {
        releaseLock(DETAIL_LOCK_PREFIX + safe(noteId), token);
    }

    public void evictPublicLists() {
        deleteByPrefix(PUBLIC_LIST_PREFIX);
    }

    public void evictAllMineLists() {
        deleteByPrefix(MINE_LIST_PREFIX);
    }

    public void evictMineLists(Long userId) {
        if (userId == null) {
            evictAllMineLists();
            return;
        }
        deleteByPrefix(MINE_LIST_PREFIX + safe(userId) + ":");
    }

    public void evictNoteDetail(Long noteId) {
        redisTemplate.delete(detailKey(noteId));
    }

    public void evictNoteComments(Long noteId) {
        redisTemplate.delete(commentsKey(noteId));
    }

    public void evictNoteReadCaches(Long noteId) {
        evictPublicLists();
        evictAllMineLists();
        evictNoteDetail(noteId);
        evictNoteComments(noteId);
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

    private Long extractNoteId(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        int lastColon = key.lastIndexOf(':');
        if (lastColon < 0 || lastColon >= key.length() - 1) {
            return null;
        }
        try {
            return Long.parseLong(key.substring(lastColon + 1));
        } catch (Exception ignore) {
            return null;
        }
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
