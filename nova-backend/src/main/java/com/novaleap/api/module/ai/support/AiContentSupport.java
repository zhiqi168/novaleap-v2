package com.novaleap.api.module.ai.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaleap.api.service.AiService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AiContentSupport {

    private static final int MAX_REJECT_REASON_LENGTH = 240;
    private static final int MAX_SUMMARY_POINT_LENGTH = 60;
    private static final int MIN_SUMMARY_POINT_LENGTH = 12;

    private static final Set<String> NOTE_BANNED_WORDS = Set.of(
            "吸毒", "毒品", "贩毒", "涉毒",
            "赌博", "博彩", "赌钱", "网赌",
            "诈骗", "洗钱", "刷单", "传销",
            "暴恐", "恐怖袭击", "爆炸物", "枪支", "制枪",
            "仇恨言论", "极端主义",
            "色情", "约炮", "成人视频", "嫖娼",
            "草泥马", "你妈", "傻逼", "操你妈"
    );

    private static final Set<String> FORBIDDEN_IDENTITY_TERMS = Set.of(
            "longcat", "美团", "openai", "chatgpt", "gpt", "claude", "anthropic"
    );

    private final ObjectMapper objectMapper;

    public AiContentSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String normalizeIdentityAnswer(String answer, String identityLine, String fallbackMessage) {
        String text = safe(answer);
        if (text.isBlank()) {
            return identityLine + fallbackMessage;
        }
        String lower = text.toLowerCase();
        for (String blocked : FORBIDDEN_IDENTITY_TERMS) {
            if (lower.contains(blocked)) {
                return identityLine + fallbackMessage;
            }
        }
        if (!text.contains("NovaLeap")) {
            return identityLine + "\n\n" + text;
        }
        return text;
    }

    public AiService.NoteModerationResult parseModerationResult(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            String json = stripJsonFence(raw.trim());
            JsonNode node = objectMapper.readTree(json);
            boolean approved = node.path("approved").asBoolean(false);
            String reason = clipReason(safe(node.path("reason").asText("")));
            if (approved) {
                return new AiService.NoteModerationResult(true, reason.isBlank() ? "AI 审核通过" : reason, "AI");
            }
            return new AiService.NoteModerationResult(false, reason.isBlank() ? "命中违规内容" : reason, "AI");
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> parseSummaryPoints(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        try {
            String json = stripJsonFence(raw.trim());
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                return Collections.emptyList();
            }
            List<String> result = new ArrayList<>();
            for (JsonNode item : node) {
                String text = clipSummaryPoint(safe(item.asText("")));
                if (!text.isBlank()) {
                    result.add(text);
                }
                if (result.size() >= 3) {
                    break;
                }
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<String> summarizeByRule(String content) {
        String clean = safe(content);
        if (clean.isBlank()) {
            return defaultNoteSummaryPoints();
        }
        String normalized = clean.replace("\r", "\n");
        String[] chunks = normalized.split("\n+");
        List<String> points = new ArrayList<>(3);
        for (String chunk : chunks) {
            String sentence = clipSummaryPoint(chunk);
            if (!sentence.isBlank()) {
                points.add(sentence);
            }
            if (points.size() >= 3) {
                break;
            }
        }
        List<String> defaults = defaultNoteSummaryPoints();
        while (points.size() < 3) {
            points.add(defaults.get(points.size()));
        }
        return points;
    }

    public List<String> matchBannedWords(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        String lowerText = text.toLowerCase();
        Set<String> hit = new LinkedHashSet<>();
        for (String word : NOTE_BANNED_WORDS) {
            if (!StringUtils.hasText(word)) {
                continue;
            }
            if (lowerText.contains(word.toLowerCase())) {
                hit.add(word);
            }
        }
        return new ArrayList<>(hit);
    }

    public List<String> defaultNoteSummaryPoints() {
        return List.of(
                "本文围绕核心主题给出背景与目标，适合作为快速入门参考。",
                "内容提炼了关键方法和操作步骤，便于直接落地实践。",
                "建议结合自身场景复盘并补充案例，持续优化执行效果。"
        );
    }

    public String clipReason(String reason) {
        String clean = safe(reason);
        if (clean.length() > MAX_REJECT_REASON_LENGTH) {
            return clean.substring(0, MAX_REJECT_REASON_LENGTH);
        }
        return clean;
    }

    private String clipSummaryPoint(String text) {
        String value = safe(text);
        if (value.length() > MAX_SUMMARY_POINT_LENGTH) {
            return value.substring(0, MAX_SUMMARY_POINT_LENGTH);
        }
        if (value.length() < MIN_SUMMARY_POINT_LENGTH && !value.isBlank()) {
            return value + "（建议补充更多细节）";
        }
        return value;
    }

    private String stripJsonFence(String value) {
        String text = safe(value);
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }
        return text.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
