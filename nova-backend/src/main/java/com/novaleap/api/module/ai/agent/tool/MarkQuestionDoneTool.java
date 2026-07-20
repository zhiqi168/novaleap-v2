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
import java.time.LocalDateTime;

/**
 * 标记题目为"已掌握/已做"。在用户完成一道题后调用，将记录写入 user_question_mastery 表。
 */
@Component
public class MarkQuestionDoneTool implements AgentTool {

    private final QuestionMapper questionMapper;
    private final UserMapper userMapper;
    private final UserQuestionMasteryMapper masteryMapper;
    private final ObjectMapper objectMapper;

    public MarkQuestionDoneTool(
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
        return "mark_question_done";
    }

    @Override
    public String getDescription() {
        return "标记一道面试题为'已掌握/已做'。用户在完成某道题后调用此工具，系统会记录该题已完成，后续推荐题目时会自动跳过。";
    }

    @Override
    public String getParameterSchema() {
        return "{\"question_id\": \"题目 ID（必填），可以从 search_questions 的结果中获取\"}";
    }

    @Override
    public String execute(String argsJson, String username) {
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            long questionId = args.path("question_id").asLong(0);

            if (questionId <= 0) {
                return "错误：缺少有效参数 question_id（题目 ID）。";
            }

            // 查找用户
            Long userId = resolveUserId(username);
            if (userId == null) {
                return "未找到用户信息，无法标记。请先登录后再试。";
            }

            // 检查题目是否存在且已上架
            Question question = questionMapper.selectById(questionId);
            if (question == null) {
                return "错误：题目 ID " + questionId + " 不存在。";
            }
            if (!Integer.valueOf(1).equals(question.getStatus())) {
                return "错误：题目 ID " + questionId + " 未上架，无法标记。";
            }

            // 检查是否已经标记过
            LambdaQueryWrapper<UserQuestionMastery> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(UserQuestionMastery::getUserId, userId);
            checkWrapper.eq(UserQuestionMastery::getQuestionId, questionId);
            checkWrapper.last("LIMIT 1");
            UserQuestionMastery existing = masteryMapper.selectOne(checkWrapper);
            if (existing != null) {
                return "题目《" + safe(question.getTitle()) + "》已经标记为已掌握了（标记时间：" + existing.getConfirmedAt() + "）。";
            }

            // 插入掌握记录
            UserQuestionMastery mastery = new UserQuestionMastery();
            mastery.setUserId(userId);
            mastery.setQuestionId(questionId);
            mastery.setConfirmedAt(LocalDateTime.now());
            mastery.setCreatedAt(LocalDateTime.now());
            mastery.setUpdatedAt(LocalDateTime.now());
            masteryMapper.insert(mastery);

            return "成功！已将《" + safe(question.getTitle()) + "》标记为已掌握。后续推荐题目时会自动跳过此题。";

        } catch (Exception e) {
            return "标记已做时出错：" + e.getMessage();
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