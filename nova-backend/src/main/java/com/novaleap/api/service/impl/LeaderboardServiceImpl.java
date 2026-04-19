package com.novaleap.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.novaleap.api.entity.Question;
import com.novaleap.api.entity.User;
import com.novaleap.api.entity.UserQuestionMastery;
import com.novaleap.api.mapper.QuestionMapper;
import com.novaleap.api.mapper.UserMapper;
import com.novaleap.api.mapper.UserQuestionMasteryMapper;
import com.novaleap.api.service.LeaderboardService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final String KEY_Q_COUNT = "nova:leaderboard:question_done";
    private static final String KEY_Q_SET_PREFIX = "nova:leaderboard:qset:";
    private static final String KEY_GAME_BEST = "nova:leaderboard:game_best";
    private static final String KEY_WISH_COUNT = "nova:leaderboard:wish_count";
    private static final String PROFILE_AVATAR_KEY_PREFIX = "nova:profile:avatar:";
    private static final String DEFAULT_AVATAR = "🙂";
    private static final int ACTIVE_USER_DISPLAY_CAP = 10;

    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final UserQuestionMasteryMapper userQuestionMasteryMapper;
    private final StringRedisTemplate redisTemplate;

    public LeaderboardServiceImpl(
            UserMapper userMapper,
            QuestionMapper questionMapper,
            UserQuestionMasteryMapper userQuestionMasteryMapper,
            StringRedisTemplate redisTemplate
    ) {
        this.userMapper = userMapper;
        this.questionMapper = questionMapper;
        this.userQuestionMasteryMapper = userQuestionMasteryMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void recordQuestionDone(String username, Long questionId) {
        User user = findRealUser(username);
        if (user == null || user.getId() == null || questionId == null || questionId <= 0) {
            return;
        }
        if (!questionExists(questionId)) {
            return;
        }
        if (hasDoneQuestion(user.getId(), questionId)) {
            return;
        }

        UserQuestionMastery mastery = new UserQuestionMastery();
        LocalDateTime now = LocalDateTime.now();
        mastery.setUserId(user.getId());
        mastery.setQuestionId(questionId);
        mastery.setConfirmedAt(now);
        mastery.setCreatedAt(now);
        mastery.setUpdatedAt(now);
        try {
            userQuestionMasteryMapper.insert(mastery);
        } catch (DuplicateKeyException ignore) {
            // Unique key keeps this write idempotent under concurrent clicks.
        }
        syncQuestionDoneToRedis(username, questionId);
    }

    @Override
    public void recordGameScore(String username, int score) {
        if (score < 0 || !isRealUser(username)) {
            return;
        }

        Double current = redisTemplate.opsForZSet().score(KEY_GAME_BEST, username);
        if (current == null || score > current) {
            redisTemplate.opsForZSet().add(KEY_GAME_BEST, username, score);
        }
    }

    @Override
    public void recordWishPost(String username) {
        if (!isRealUser(username)) {
            return;
        }
        redisTemplate.opsForZSet().incrementScore(KEY_WISH_COUNT, username, 1D);
    }

    @Override
    public Map<String, Object> getLeaderboard() {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "USER")
                        .orderByAsc(User::getCreatedAt)
        );
        Map<Long, Long> questionDoneMap = loadQuestionDoneCountMap();

        Map<String, Long> nicknameCount = new HashMap<>();
        for (User user : users) {
            String username = user.getUsername();
            String nickname = StringUtils.hasText(user.getNickname()) ? user.getNickname() : username;
            nicknameCount.put(nickname, nicknameCount.getOrDefault(nickname, 0L) + 1L);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        long totalQuestionDone = 0;
        long totalWishCount = 0;
        int maxGameScore = 0;

        for (User user : users) {
            String username = user.getUsername();
            long dbQuestionDone = questionDoneMap.getOrDefault(user.getId(), 0L);
            long redisQuestionDone = score(KEY_Q_COUNT, username);
            long questionDone = Math.max(dbQuestionDone, redisQuestionDone);
            long wishCount = score(KEY_WISH_COUNT, username);
            int gameBest = (int) score(KEY_GAME_BEST, username);

            long totalScore = calcTotalScore(questionDone, wishCount, gameBest);
            String nickname = StringUtils.hasText(user.getNickname()) ? user.getNickname() : username;
            String displayNickname = nickname;
            if (nicknameCount.getOrDefault(nickname, 0L) > 1 && !nickname.equals(username)) {
                displayNickname = nickname + "(" + username + ")";
            }

            Map<String, Object> row = new HashMap<>();
            row.put("userId", user.getId());
            row.put("username", username);
            row.put("nickname", displayNickname);
            row.put("avatar", avatarOf(username));
            row.put("questionDone", questionDone);
            row.put("wishCount", wishCount);
            row.put("gameBestScore", gameBest);
            row.put("totalScore", totalScore);
            list.add(row);

            totalQuestionDone += questionDone;
            totalWishCount += wishCount;
            maxGameScore = Math.max(maxGameScore, gameBest);
        }

        list.sort((a, b) -> {
            long scoreA = toLong(a.get("totalScore"));
            long scoreB = toLong(b.get("totalScore"));
            if (scoreA != scoreB) {
                return Long.compare(scoreB, scoreA);
            }
            long qa = toLong(a.get("questionDone"));
            long qb = toLong(b.get("questionDone"));
            if (qa != qb) {
                return Long.compare(qb, qa);
            }
            long ga = toLong(a.get("gameBestScore"));
            long gb = toLong(b.get("gameBestScore"));
            return Long.compare(gb, ga);
        });

        for (int i = 0; i < list.size(); i++) {
            list.get(i).put("rank", i + 1);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("activeUsers", Math.min(users.size(), ACTIVE_USER_DISPLAY_CAP));
        summary.put("questionDoneTotal", totalQuestionDone);
        summary.put("wishTotal", totalWishCount);
        summary.put("maxGameScore", maxGameScore);

        Map<String, Object> data = new HashMap<>();
        data.put("summary", summary);
        data.put("list", list);
        return data;
    }

    @Override
    public Map<String, Object> getUserSnapshot(String username) {
        User user = findRealUser(username);
        if (user == null || user.getId() == null) {
            return Collections.emptyMap();
        }

        Map<Long, Long> questionDoneMap = loadQuestionDoneCountMap();
        long dbQuestionDone = questionDoneMap.getOrDefault(user.getId(), 0L);
        long redisQuestionDone = score(KEY_Q_COUNT, username);
        long questionDone = Math.max(dbQuestionDone, redisQuestionDone);
        long wishCount = score(KEY_WISH_COUNT, username);
        int gameBest = (int) score(KEY_GAME_BEST, username);

        Map<String, Object> row = new HashMap<>();
        row.put("userId", user.getId());
        row.put("username", username);
        row.put("nickname", StringUtils.hasText(user.getNickname()) ? user.getNickname() : username);
        row.put("avatar", avatarOf(username));
        row.put("questionDone", questionDone);
        row.put("wishCount", wishCount);
        row.put("gameBestScore", gameBest);
        row.put("totalScore", calcTotalScore(questionDone, wishCount, gameBest));
        return row;
    }

    @Override
    public Set<Long> getDoneQuestionIds(String username) {
        User user = findRealUser(username);
        if (user == null || user.getId() == null) {
            return Collections.emptySet();
        }

        Set<Long> ids = new LinkedHashSet<>();
        List<UserQuestionMastery> rows = userQuestionMasteryMapper.selectList(
                new LambdaQueryWrapper<UserQuestionMastery>()
                        .eq(UserQuestionMastery::getUserId, user.getId())
                        .orderByAsc(UserQuestionMastery::getCreatedAt)
        );
        if (rows != null && !rows.isEmpty()) {
            for (UserQuestionMastery row : rows) {
                if (row.getQuestionId() != null && row.getQuestionId() > 0) {
                    ids.add(row.getQuestionId());
                }
            }
        }
        Set<String> redisMembers = redisTemplate.opsForSet().members(KEY_Q_SET_PREFIX + username);
        if (redisMembers != null && !redisMembers.isEmpty()) {
            for (String item : redisMembers) {
                Long id = parseLong(item);
                if (id != null && id > 0) {
                    ids.add(id);
                }
            }
        }
        return ids.isEmpty() ? Collections.emptySet() : ids;
    }

    private boolean isRealUser(String username) {
        return findRealUser(username) != null;
    }

    private User findRealUser(String username) {
        if (!StringUtils.hasText(username) || username.startsWith("guest_")) {
            return null;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            return null;
        }
        String role = StringUtils.hasText(user.getRole()) ? user.getRole().trim().toUpperCase() : "";
        if ("GUEST".equals(role)) {
            return null;
        }
        return user;
    }

    private String avatarOf(String username) {
        String avatar = redisTemplate.opsForValue().get(PROFILE_AVATAR_KEY_PREFIX + username);
        return StringUtils.hasText(avatar) ? avatar : DEFAULT_AVATAR;
    }

    private boolean questionExists(Long questionId) {
        Long count = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getId, questionId)
                        .eq(Question::getStatus, 1)
        );
        return count != null && count > 0;
    }

    private boolean hasDoneQuestion(Long userId, Long questionId) {
        Long count = userQuestionMasteryMapper.selectCount(
                new LambdaQueryWrapper<UserQuestionMastery>()
                        .eq(UserQuestionMastery::getUserId, userId)
                        .eq(UserQuestionMastery::getQuestionId, questionId)
        );
        return count != null && count > 0;
    }

    private Map<Long, Long> loadQuestionDoneCountMap() {
        QueryWrapper<UserQuestionMastery> wrapper = new QueryWrapper<>();
        wrapper.select("user_id", "COUNT(*) AS question_done_count");
        wrapper.groupBy("user_id");

        List<Map<String, Object>> rows = userQuestionMasteryMapper.selectMaps(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long userId = parseLong(row.get("user_id"));
            Long count = parseLong(row.get("question_done_count"));
            if (userId != null && userId > 0) {
                result.put(userId, count == null ? 0L : Math.max(0L, count));
            }
        }
        return result;
    }

    private void syncQuestionDoneToRedis(String username, Long questionId) {
        if (!StringUtils.hasText(username) || questionId == null || questionId <= 0) {
            return;
        }
        String setKey = KEY_Q_SET_PREFIX + username;
        Long added = redisTemplate.opsForSet().add(setKey, String.valueOf(questionId));
        if (added != null && added > 0) {
            redisTemplate.opsForZSet().incrementScore(KEY_Q_COUNT, username, 1D);
        }
    }

    private long score(String key, String username) {
        Double s = redisTemplate.opsForZSet().score(key, username);
        return s == null ? 0L : Math.round(s);
    }

    private long calcTotalScore(long questionDone, long wishCount, int gameBest) {
        long questionPart = questionDone * 20;
        long wishPart = wishCount * 16;
        long gamePart = Math.min(1200, gameBest);
        return questionPart + wishPart + gamePart;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
