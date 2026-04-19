package com.novaleap.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.novaleap.api.dto.WishCommentVO;
import com.novaleap.api.dto.WishLikeVO;
import com.novaleap.api.dto.WishRequest;
import com.novaleap.api.dto.WishWallItemVO;
import com.novaleap.api.entity.Wish;
import com.novaleap.api.entity.WishComment;
import com.novaleap.api.entity.WishLike;
import com.novaleap.api.mapper.WishCommentMapper;
import com.novaleap.api.mapper.WishLikeMapper;
import com.novaleap.api.mapper.WishMapper;
import com.novaleap.api.module.wish.support.WishQueueConstants;
import com.novaleap.api.service.LeaderboardService;
import com.novaleap.api.service.WishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WishServiceImpl extends ServiceImpl<WishMapper, Wish> implements WishService {

    private static final int WISH_STATUS_PENDING = 0;
    private static final int WISH_STATUS_APPROVED = 1;
    private static final String DEFAULT_WISH_CITY = "来自星海";

    private final StringRedisTemplate redisTemplate;
    private final LeaderboardService leaderboardService;
    private final WishLikeMapper wishLikeMapper;
    private final WishCommentMapper wishCommentMapper;

    public WishServiceImpl(
            StringRedisTemplate redisTemplate,
            LeaderboardService leaderboardService,
            WishLikeMapper wishLikeMapper,
            WishCommentMapper wishCommentMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.leaderboardService = leaderboardService;
        this.wishLikeMapper = wishLikeMapper;
        this.wishCommentMapper = wishCommentMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(WishRequest request, String username) {
        Wish wish = new Wish();
        wish.setContent(request.getContent());
        wish.setCity(StringUtils.hasText(request.getCity()) ? request.getCity() : DEFAULT_WISH_CITY);
        wish.setStatus(WISH_STATUS_PENDING);
        wish.setCreatedAt(LocalDateTime.now());
        this.save(wish);

        runAfterCommit(() -> {
            enqueuePendingWish(wish.getId());
            if (StringUtils.hasText(username)) {
                try {
                    leaderboardService.recordWishPost(username);
                } catch (Exception e) {
                    log.warn("record wish leaderboard failed for {}: {}", username, e.getMessage());
                }
            }
        });
    }

    @Override
    public void enqueuePendingWish(Long wishId) {
        if (wishId == null) {
            return;
        }
        String wishIdStr = String.valueOf(wishId);
        try {
            Long added = redisTemplate.opsForSet().add(WishQueueConstants.PENDING_INDEX_SET, wishIdStr);
            if (added == null || added <= 0) {
                return;
            }
            Long pushed = redisTemplate.opsForList().rightPush(WishQueueConstants.PENDING_QUEUE, wishIdStr);
            if (pushed == null) {
                redisTemplate.opsForSet().remove(WishQueueConstants.PENDING_INDEX_SET, wishIdStr);
            }
        } catch (Exception e) {
            redisTemplate.opsForSet().remove(WishQueueConstants.PENDING_INDEX_SET, wishIdStr);
            log.error("enqueue pending wish failed, wishId={}", wishId, e);
        }
    }

    @Override
    public Wish getPendingWish(Long wishId) {
        if (wishId == null) {
            return null;
        }
        LambdaQueryWrapper<Wish> query = new LambdaQueryWrapper<>();
        query.eq(Wish::getId, wishId)
                .eq(Wish::getStatus, WISH_STATUS_PENDING)
                .last("LIMIT 1");
        return this.getOne(query, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeAutoReview(Long wishId, String emotion, String color, double speed, int posX, int posY) {
        Wish wish = getPendingWish(wishId);
        if (wish == null) {
            return false;
        }
        wish.setEmotion(emotion);
        wish.setColor(color);
        wish.setFloatSpeed(speed);
        wish.setPosX(posX);
        wish.setPosY(posY);
        wish.setStatus(WISH_STATUS_APPROVED);
        return this.updateById(wish);
    }

    @Override
    public List<Long> listPendingWishIds(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        LambdaQueryWrapper<Wish> query = new LambdaQueryWrapper<>();
        query.select(Wish::getId)
                .eq(Wish::getStatus, WISH_STATUS_PENDING)
                .orderByAsc(Wish::getCreatedAt)
                .last("LIMIT " + safeLimit);
        List<Wish> wishes = this.list(query);
        if (wishes == null || wishes.isEmpty()) {
            return Collections.emptyList();
        }
        return wishes.stream()
                .map(Wish::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<WishWallItemVO> listApproved(String actorType, String actorId) {
        LambdaQueryWrapper<Wish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Wish::getStatus, WISH_STATUS_APPROVED)
                .orderByDesc(Wish::getCreatedAt)
                .last("LIMIT 50");
        List<Wish> wishes = this.list(wrapper);
        if (wishes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> wishIds = wishes.stream().map(Wish::getId).filter(Objects::nonNull).toList();
        Map<Long, Long> likeCountMap = queryLikeCounts(wishIds);
        Map<Long, Long> commentCountMap = queryCommentCounts(wishIds);
        Set<Long> likedWishIds = queryLikedWishIds(wishIds, actorType, actorId);

        List<WishWallItemVO> result = new ArrayList<>(wishes.size());
        for (Wish wish : wishes) {
            WishWallItemVO vo = new WishWallItemVO();
            vo.setId(wish.getId());
            vo.setContent(wish.getContent());
            vo.setEmotion(wish.getEmotion());
            vo.setColor(wish.getColor());
            vo.setCity(wish.getCity());
            vo.setPosX(wish.getPosX());
            vo.setPosY(wish.getPosY());
            vo.setFloatSpeed(wish.getFloatSpeed());
            vo.setStatus(wish.getStatus());
            vo.setCreatedAt(wish.getCreatedAt());
            vo.setLikeCount(likeCountMap.getOrDefault(wish.getId(), 0L));
            vo.setCommentCount(commentCountMap.getOrDefault(wish.getId(), 0L));
            vo.setLikedByMe(likedWishIds.contains(wish.getId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    public WishLikeVO toggleLike(Long wishId, String actorType, String actorId) {
        if (wishId == null) {
            throw new IllegalArgumentException("愿望ID不能为空");
        }
        if (!StringUtils.hasText(actorType) || !StringUtils.hasText(actorId)) {
            throw new IllegalArgumentException("访问者身份标识不能为空");
        }
        requireApprovedWish(wishId);

        LambdaQueryWrapper<WishLike> query = new LambdaQueryWrapper<>();
        query.eq(WishLike::getWishId, wishId)
                .eq(WishLike::getActorType, actorType.trim())
                .eq(WishLike::getActorId, actorId.trim())
                .last("LIMIT 1");
        WishLike existed = wishLikeMapper.selectOne(query);
        boolean liked;
        if (existed != null) {
            wishLikeMapper.deleteById(existed.getId());
            liked = false;
        } else {
            WishLike wishLike = new WishLike();
            wishLike.setWishId(wishId);
            wishLike.setActorType(actorType.trim());
            wishLike.setActorId(actorId.trim());
            wishLike.setCreatedAt(LocalDateTime.now());
            try {
                wishLikeMapper.insert(wishLike);
            } catch (DuplicateKeyException ignore) {
                // Concurrent insert, keep liked=true
            }
            liked = true;
        }

        long likeCount = wishLikeMapper.selectCount(
                new LambdaQueryWrapper<WishLike>().eq(WishLike::getWishId, wishId)
        );
        WishLikeVO vo = new WishLikeVO();
        vo.setWishId(wishId);
        vo.setLiked(liked);
        vo.setLikeCount(likeCount);
        return vo;
    }

    @Override
    public List<WishCommentVO> listComments(Long wishId, String currentUsername) {
        if (wishId == null) {
            throw new IllegalArgumentException("愿望ID不能为空");
        }
        requireApprovedWish(wishId);

        LambdaQueryWrapper<WishComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WishComment::getWishId, wishId)
                .orderByDesc(WishComment::getCreatedAt)
                .orderByDesc(WishComment::getId);
        List<WishComment> comments = wishCommentMapper.selectList(wrapper);

        return comments.stream()
                .map(comment -> toCommentVO(comment, currentUsername))
                .collect(Collectors.toList());
    }

    @Override
    public WishCommentVO addComment(Long wishId, Long userId, String username, String nickname, String content) {
        if (wishId == null) {
            throw new IllegalArgumentException("愿望ID不能为空");
        }
        if (userId == null || !StringUtils.hasText(username)) {
            throw new IllegalArgumentException("发表评论前请先登录");
        }
        String normalizedContent = normalizeContent(content);
        if (!StringUtils.hasText(normalizedContent)) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        requireApprovedWish(wishId);

        WishComment comment = new WishComment();
        comment.setWishId(wishId);
        comment.setUserId(userId);
        comment.setUsername(username.trim());
        comment.setNickname(StringUtils.hasText(nickname) ? nickname.trim() : username.trim());
        comment.setContent(normalizedContent);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setDeleted(0);
        wishCommentMapper.insert(comment);
        return toCommentVO(comment, username);
    }

    private WishCommentVO toCommentVO(WishComment comment, String currentUsername) {
        WishCommentVO vo = new WishCommentVO();
        vo.setId(comment.getId());
        vo.setWishId(comment.getWishId());
        vo.setUserId(comment.getUserId());
        vo.setUsername(comment.getUsername());
        vo.setNickname(comment.getNickname());
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setMine(StringUtils.hasText(currentUsername)
                && currentUsername.trim().equalsIgnoreCase(String.valueOf(comment.getUsername())));
        return vo;
    }

    private Wish requireApprovedWish(Long wishId) {
        LambdaQueryWrapper<Wish> query = new LambdaQueryWrapper<>();
        query.eq(Wish::getId, wishId)
                .eq(Wish::getStatus, WISH_STATUS_APPROVED)
                .last("LIMIT 1");
        Wish wish = this.getOne(query);
        if (wish == null) {
            throw new IllegalArgumentException("未找到该愿望");
        }
        return wish;
    }

    private Map<Long, Long> queryLikeCounts(List<Long> wishIds) {
        if (wishIds == null || wishIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<WishLike> wrapper = new QueryWrapper<>();
        wrapper.select("wish_id AS wishId", "COUNT(*) AS total")
                .in("wish_id", wishIds)
                .groupBy("wish_id");
        List<Map<String, Object>> rows = wishLikeMapper.selectMaps(wrapper);
        return toCountMap(rows);
    }

    private Map<Long, Long> queryCommentCounts(List<Long> wishIds) {
        if (wishIds == null || wishIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<WishComment> wrapper = new QueryWrapper<>();
        wrapper.select("wish_id AS wishId", "COUNT(*) AS total")
                .in("wish_id", wishIds)
                .eq("deleted", 0)
                .groupBy("wish_id");
        List<Map<String, Object>> rows = wishCommentMapper.selectMaps(wrapper);
        return toCountMap(rows);
    }

    private Set<Long> queryLikedWishIds(List<Long> wishIds, String actorType, String actorId) {
        if (wishIds == null || wishIds.isEmpty() || !StringUtils.hasText(actorType) || !StringUtils.hasText(actorId)) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<WishLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WishLike::getWishId, wishIds)
                .eq(WishLike::getActorType, actorType.trim())
                .eq(WishLike::getActorId, actorId.trim());
        List<WishLike> likedRows = wishLikeMapper.selectList(wrapper);
        if (likedRows == null || likedRows.isEmpty()) {
            return Collections.emptySet();
        }
        return likedRows.stream()
                .map(WishLike::getWishId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Long, Long> toCountMap(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long wishId = toLong(firstNonNull(
                    row.get("wishId"),
                    row.get("wish_id"),
                    row.get("WISHID"),
                    row.get("WISH_ID")
            ));
            Long total = toLong(firstNonNull(
                    row.get("total"),
                    row.get("TOTAL"),
                    row.get("count"),
                    row.get("COUNT(*)")
            ));
            if (wishId != null) {
                map.put(wishId, total == null ? 0L : total);
            }
        }
        return map;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private void runAfterCommit(Runnable action) {
        if (action == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String val = content.trim();
        return val.length() > 300 ? val.substring(0, 300) : val;
    }
}
