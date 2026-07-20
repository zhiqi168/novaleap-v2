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
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 搜索题库中的面试题目。
 * 按关键词搜索题目标题，可选分类和难度过滤。
 */
@Component
public class SearchQuestionsTool implements AgentTool {

    private static final int MAX_RESULTS = 6;

    private final QuestionMapper questionMapper;
    private final UserMapper userMapper;
    private final UserQuestionMasteryMapper masteryMapper;
    private final ObjectMapper objectMapper;

    public SearchQuestionsTool(QuestionMapper questionMapper, UserMapper userMapper, UserQuestionMasteryMapper masteryMapper, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.userMapper = userMapper;
        this.masteryMapper = masteryMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "search_questions";
    }

    @Override
    public String getDescription() {
        return "搜索题库中的面试题目。当你需要查找特定技术领域的面试题时使用，支持按关键词、分类和难度筛选。";
    }

    @Override
    public String getParameterSchema() {
        return "{\"keyword\": \"搜索关键词（必填）\", \"category\": \"分类筛选，可选值: java/spring/db/redis/algo/network/system-design/linux/arch（可选）\", \"difficulty\": \"难度筛选，1=简单 2=中等 3=困难（可选）\"}";
    }

    @Override
    public String execute(String argsJson, String username) {
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            String keyword = safe(args.path("keyword").asText(""));
            String category = safe(args.path("category").asText(""));
            int difficulty = args.path("difficulty").asInt(0);

            if (keyword.isBlank()) {
                return "错误：缺少必要参数 keyword（搜索关键词）。";
            }

            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Question::getStatus, 1);
            wrapper.like(Question::getTitle, keyword.trim());

            if (!category.isBlank()) {
                wrapper.eq(Question::getCategory, category.trim());
            }
            if (difficulty >= 1 && difficulty <= 3) {
                wrapper.eq(Question::getDifficulty, difficulty);
            }

            wrapper.orderByDesc(Question::getViewCount);
            wrapper.last("LIMIT " + MAX_RESULTS);

            List<Question> questions = questionMapper.selectList(wrapper);

            if (questions.isEmpty()) {
                return "未找到与 \"" + keyword + "\" 相关的题目。";
            }

            // 查询用户已掌握状态
            Set<Long> masteredIds = resolveMasteredIds(username);

            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(questions.size()).append(" 道相关题目：\n");
            int idx = 1;
            for (Question q : questions) {
                String cat = safe(q.getCategory()).isBlank() ? "未分类" : q.getCategory();
                String diff = switch (q.getDifficulty() != null ? q.getDifficulty() : 0) {
                    case 1 -> "简单";
                    case 2 -> "中等";
                    case 3 -> "困难";
                    default -> "未知";
                };
                String status = masteredIds.contains(q.getId()) ? " ✅ 已掌握" : "";
                sb.append(idx++).append(". [").append(cat).append("] ");
                sb.append(safe(q.getTitle())).append("（难度：").append(diff).append("）").append(status).append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            return "搜索题目时出错：" + e.getMessage();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private Set<Long> resolveMasteredIds(String username) {
        if (username == null || username.isBlank()) return Set.of();
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getUsername, username.trim());
        userWrapper.last("LIMIT 1");
        User user = userMapper.selectOne(userWrapper);
        if (user == null) return Set.of();

        LambdaQueryWrapper<UserQuestionMastery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserQuestionMastery::getUserId, user.getId());
        List<UserQuestionMastery> records = masteryMapper.selectList(wrapper);
        return records.stream().map(UserQuestionMastery::getQuestionId).collect(Collectors.toSet());
    }
}