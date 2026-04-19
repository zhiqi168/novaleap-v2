package com.novaleap.api.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface AiService {

    record NoteModerationResult(boolean approved, String reason, String source) {}

    /**
     * 针对某一道题目给出 AI 解析 (流式)
     */
    SseEmitter explainQuestion(String username, Long questionId);

    /**
     * AI 陪练对话 (流式)
     * @param message 用户说的内容
     * @param topic 面试主题
     */
    SseEmitter coachChat(String username, String message, String topic, String image, String mode);

    List<Map<String, Object>> getCoachHistory(String username, int limit);

    String createCoachSession(String username);

    void clearCoachHistory(String username);

    /**
     * AI 简历分析 (流式)
     * @param username 用户名
     * @param resumeText 简历文本
     * @param targetRole 目标岗位
     */
    SseEmitter analyzeResume(String username, String resumeText, String targetRole);

    /**
     * AI 手记全文摘要（非流式）
     * @param username 用户名
     * @param title 手记标题
     * @param noteContent 手记全文
     */
    List<String> summarizeNote(String username, String title, String noteContent);

    String generateDailyQuote(String username, String nickname);

    /**
     * AI + 关键词审核投稿手记
     */
    NoteModerationResult moderateNote(String title, String noteContent);
}
