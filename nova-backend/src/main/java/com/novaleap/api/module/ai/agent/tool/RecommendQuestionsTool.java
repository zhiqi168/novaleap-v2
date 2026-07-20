package com.novaleap.api.module.ai.agent.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaleap.api.entity.Question;
import com.novaleap.api.entity.User;
import com.novaleap.api.entity.UserQuestionMastery;
import com.novaleap.api.mapper.QuestionMapper;
import com.novaleap.api.mapper.UserMapper;
import com.novaleap.api.mapper.UserQuestionMasteryMapper;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 根据用户薄弱点推荐题目。
 * 分析用户的答题掌握记录（user_question_mastery），找出掌握率低的分类，从中推荐题目。
 */
@Component
public class RecommendQuestionsTool implements AgentTool {

    private static final int DEFAULT_COUNT = 5;
    private static final int MAX_COUNT = 10;

    private final QuestionMapper questionMapper;
    private final UserMapper userMapper;
    private final UserQuestionMasteryMapper masteryMapper;
    private final ObjectMapper objectMapper;

    public RecommendQuestionsTool(
            QuestionMapper questionMapper,
            UserMapper userMapper,
            UserQuestionMasteryMapper masteryMapper,
            ObjectMapper objectMapper
    ) {
        this.questionMapper = questionMapper;
        this.userMapper = userMapper;
        this.masteryMapper = masteryMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "recommend_questions";
    }

    @Override
    public String getDescription() {
        return "根据用户的薄弱知识点推荐题目。分析用户的历史掌握记录，找出掌握率低的分类并推荐题目。";
    }

    @Override
    public String getParameterSchema() {
        return "{\"count\": \"推荐题目数量（可选，默认5，最大10）\"}";
    }

    @Override
    public String execute(String argsJson, String username) {
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            int count = args.path("count").asInt(DEFAULT_COUNT);
            if (count < 1) count = DEFAULT_COUNT;
            if (count > MAX_COUNT) count = MAX_COUNT;

            Long userId = resolveUserId(username);
            if (userId == null) {
                return "未找到用户信息，无法推荐题目。";
            }

            // 获取用户已掌握的题目 ID 集合
            LambdaQueryWrapper<UserQuestionMastery> masteryWrapper = new LambdaQueryWrapper<>();
            masteryWrapper.eq(UserQuestionMastery::getUserId, userId);
            List<UserQuestionMastery> mastered = masteryMapper.selectList(masteryWrapper);
            Set<Long> masteredIds = mastered.stream()
                    .map(UserQuestionMastery::getQuestionId)
                    .collect(Collectors.toSet());

            // 获取所有上架的官方题目
            LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<>();
            questionWrapper.eq(Question::getStatus, 1);
            List<Question> allQuestions = questionMapper.selectList(questionWrapper);

            if (allQuestions.isEmpty()) {
                return "题库暂无可用题目。";
            }

            // 按分类统计
            java.util.Map<String, java.util.List<Question>> byCategory = allQuestions.stream()
                    .collect(Collectors.groupingBy(
                            q -> q.getCategory() != null ? q.getCategory() : "未分类"
                    ));

            // 找出薄弱分类：题目总数多但掌握少的分类优先
            java.util.List<String> weakCategories = byCategory.entrySet().stream()
                    .sorted((a, b) -> {
                        long aTotal = a.getValue().size();
                        long aMastered = a.getValue().stream().filter(q -> masteredIds.contains(q.getId())).count();
                        double aRate = aTotal > 0 ? (double) aMastered / aTotal : 1.0;

                        long bTotal = b.getValue().size();
                        long bMastered = b.getValue().stream().filter(q -> masteredIds.contains(q.getId())).count();
                        double bRate = bTotal > 0 ? (double) bMastered / bTotal : 1.0;

                        int rateCmp = Double.compare(aRate, bRate);
                        if (rateCmp != 0) return rateCmp;
                        return Long.compare(bTotal, aTotal);
                    })
                    .map(java.util.Map.Entry::getKey)
                    .collect(Collectors.toList());

            // 从薄弱分类中选未掌握的题目
            Random random = new Random();
            Set<Question> recommended = new HashSet<>();
            for (String cat : weakCategories) {
                List<Question> candidates = byCategory.get(cat).stream()
                        .filter(q -> !masteredIds.contains(q.getId()))
                        .collect(Collectors.toList());
                if (candidates.isEmpty()) continue;

                // 从该分类随机选 1-2 题
                int take = Math.min(2, candidates.size());
                for (int i = 0; i < take && recommended.size() < count; i++) {
                    Question pick = candidates.get(random.nextInt(candidates.size()));
                    recommended.add(pick);
                    candidates.remove(pick);
                }
                if (recommended.size() >= count) break;
            }

            // 如果还不够，从未掌握中随便补
            if (recommended.size() < count) {
                List<Question> remaining = allQuestions.stream()
                        .filter(q -> !masteredIds.contains(q.getId()) && !recommended.contains(q))
                        .collect(Collectors.toList());
                while (recommended.size() < count && !remaining.isEmpty()) {
                    Question pick = remaining.remove(random.nextInt(remaining.size()));
                    recommended.add(pick);
                }
            }

            if (recommended.isEmpty()) {
                return "你已经掌握了所有可用题目！可以尝试出一些新题或切换到新分类。";
            }

            // 生成薄弱分类总结
            StringBuilder sb = new StringBuilder();
            sb.append("根据你的掌握情况，推荐以下 ").append(recommended.size()).append(" 道题目：\n\n");
            int idx = 1;
            for (Question q : recommended) {
                String cat = q.getCategory() != null ? q.getCategory() : "未分类";
                String diff = switch (q.getDifficulty() != null ? q.getDifficulty() : 0) {
                    case 1 -> "简单";
                    case 2 -> "中等";
                    case 3 -> "困难";
                    default -> "未知";
                };
                sb.append(idx++).append(". [").append(cat).append("] ");
                sb.append(q.getTitle() != null ? q.getTitle().trim() : "").append("（难度：").append(diff).append("）\n");
            }

            // 补充薄弱分类信息
            sb.append("\n薄弱分类分析：\n");
            int shown = 0;
            for (String cat : weakCategories) {
                if (shown >= 3) break;
                long total = byCategory.get(cat).size();
                long masteredCount = byCategory.get(cat).stream().filter(q -> masteredIds.contains(q.getId())).count();
                double rate = total > 0 ? (double) masteredCount / total * 100 : 0;
                sb.append("- ").append(cat).append("：已掌握 ").append(masteredCount).append("/").append(total).append(" 题（").append(String.format("%.0f%%", rate)).append("）\n");
                shown++;
            }

            return sb.toString();

        } catch (Exception e) {
            return "推荐题目时出错：" + e.getMessage();
        }
    }

    private Long resolveUserId(String username) {
        if (username == null || username.isBlank()) return null;
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username.trim());
        wrapper.last("LIMIT 1");
        User user = userMapper.selectOne(wrapper);
        return user != null ? user.getId() : null;
    }
}