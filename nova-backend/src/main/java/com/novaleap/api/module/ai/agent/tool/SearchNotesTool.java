package com.novaleap.api.module.ai.agent.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaleap.api.entity.Note;
import com.novaleap.api.entity.User;
import com.novaleap.api.mapper.NoteMapper;
import com.novaleap.api.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.List;

/**
 * 搜索用户的笔记。
 * 根据关键词搜索当前用户已发布的笔记标题或内容。
 */
@Component
public class SearchNotesTool implements AgentTool {

    private static final int MAX_RESULTS = 5;

    private final NoteMapper noteMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public SearchNotesTool(NoteMapper noteMapper, UserMapper userMapper, ObjectMapper objectMapper) {
        this.noteMapper = noteMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "search_notes";
    }

    @Override
    public String getDescription() {
        return "搜索当前用户的笔记/手记。当用户说'搜一下我的笔记'、'找找我的手记'、'查一下关于XX的笔记'时使用，按关键词搜索笔记标题或内容。注意：搜索题库请用 search_questions，搜索笔记请用此工具。";
    }

    @Override
    public String getParameterSchema() {
        return "{\"keyword\": \"搜索关键词（必填）\"}";
    }

    @Override
    public String execute(String argsJson, String username) {
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            String keyword = safe(args.path("keyword").asText(""));

            if (keyword.isBlank()) {
                return "错误：缺少必要参数 keyword（搜索关键词）。";
            }

            // 通过 username 查找 userId
            Long userId = resolveUserId(username);
            if (userId == null) {
                return "未找到用户信息，无法搜索笔记。";
            }

            LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Note::getUserId, userId);
            // 不限制 status，自己的笔记全部能搜到（包含待审核）
            wrapper.and(w -> w
                    .like(Note::getTitle, keyword.trim())
                    .or()
                    .like(Note::getContent, keyword.trim())
            );
            wrapper.orderByDesc(Note::getCreatedAt);
            wrapper.last("LIMIT " + MAX_RESULTS);

            List<Note> notes = noteMapper.selectList(wrapper);

            if (notes.isEmpty()) {
                return "未找到与 \"" + keyword + "\" 相关的笔记。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(notes.size()).append(" 条相关笔记：\n");
            int idx = 1;
            for (Note note : notes) {
                String summary = safe(note.getSummary()).isBlank()
                        ? truncate(safe(note.getContent()), 60)
                        : safe(note.getSummary());
                sb.append(idx++).append(". ").append(safe(note.getTitle())).append("\n");
                sb.append("   摘要：").append(summary).append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            return "搜索笔记时出错：" + e.getMessage();
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

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}