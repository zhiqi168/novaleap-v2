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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 查看用户的已掌握/已做题目清单。
 */
@Component
public class GetMyMasteryTool implements AgentTool {

    private final UserQuestionMasteryMapper masteryMapper;
    private final QuestionMapper questionMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public GetMyMasteryTool(
            UserQuestionMasteryMapper masteryMapper,
            QuestionMapper questionMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper
    ) {
        this.masteryMapper = masteryMapper;
        this.questionMapper = questionMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_my_mastery";
    }

    @Override
    public String getDescription() {
        return "查看当前用户已掌握/已做的题目清单，按标记时间倒序排列。当用户问'我做过哪些题'、'已做清单'时调用。";
    }

    @Override
    public String getParameterSchema() {
        return "{}";
    }

    @Override
    public String execute(String argsJson, String username) {
        try {
            Long userId = resolveUserId(username);
            if (userId == null) {
                return "未找到用户信息，请先登录后再试。";
            }

            // 查询用户的掌握记录
            LambdaQueryWrapper<UserQuestionMastery> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserQuestionMastery::getUserId, userId);
            wrapper.orderByDesc(UserQuestionMastery::getConfirmedAt);
            List<UserQuestionMastery> records = masteryMapper.selectList(wrapper);

            if (records.isEmpty()) {
                return "你还没有标记过已掌握的题目。你可以先搜索题目，然后使用 mark_question_done 工具标记已做。";
            }

            // 批量查询题目信息
            List<Long> questionIds = records.stream()
                    .map(UserQuestionMastery::getQuestionId)
                    .collect(Collectors.toList());

            Map<Long, Question> questionMap;
            if (!questionIds.isEmpty()) {
                List<Question> questions = questionMapper.selectBatchIds(questionIds);
                questionMap = questions.stream()
                        .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
            } else {
                questionMap = Map.of();
            }

            // 按分类统计
            Map<String, Long> categoryCount = records.stream()
                    .map(r -> questionMap.get(r.getQuestionId()))
                    .filter(q -> q != null)
                    .collect(Collectors.groupingBy(
                            q -> q.getCategory() != null ? q.getCategory() : "未分类",
                            Collectors.counting()
                    ));

            // 生成清单
            StringBuilder sb = new StringBuilder();
            sb.append("你已掌握 ").append(records.size()).append(" 道题目：\n\n");

            int idx = 1;
            for (UserQuestionMastery record : records) {
                Question q = questionMap.get(record.getQuestionId());
                String title = q != null ? safe(q.getTitle()) : "（题目 ID: " + record.getQuestionId() + "，可能已删除）";
                String cat = q != null && q.getCategory() != null ? q.getCategory() : "未分类";
                String diff = q != null ? switch (q.getDifficulty() != null ? q.getDifficulty() : 0) {
                    case 1 -> "简单";
                    case 2 -> "中等";
                    case 3 -> "困难";
                    default -> "未知";
                } : "未知";
                String time = record.getConfirmedAt() != null ? record.getConfirmedAt().toString().substring(0, 10) : "未知";
                sb.append(idx++).append(". [").append(cat).append("] ").append(title);
                sb.append("（难度：").append(diff).append("，完成时间：").append(time).append("）\n");
            }

            // 分类统计
            sb.append("\n按分类统计：\n");
            for (Map.Entry<String, Long> entry : categoryCount.entrySet()) {
                sb.append("- ").append(entry.getKey()).append("：").append(entry.getValue()).append(" 题\n");
            }

            return sb.toString();

        } catch (Exception e) {
            return "查询已做清单时出错：" + e.getMessage();
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}