package com.novaleap.api.module.ai.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AiIdentitySupport {

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(?:\\d{1,3}\\.){3}\\d{1,3}$");
    private static final Set<String> IDENTITY_QUESTION_HINTS = Set.of(
            "who are you",
            "developed by",
            "openai",
            "chatgpt",
            "gpt",
            "你是谁",
            "你是什么模型",
            "谁研发",
            "哪家公司",
            "自研"
    );
    private static final Set<String> CREATOR_QUESTION_HINTS = Set.of(
            "creator",
            "created by",
            "创作者",
            "开发者",
            "谁做的",
            "谁开发的",
            "张志琪",
            "张志奇",
            "张子琪",
            "谁是创作者"
    );

    public String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public boolean isIdentityQuestion(String message) {
        String text = safe(message).toLowerCase();
        if (text.isBlank()) {
            return false;
        }
        return IDENTITY_QUESTION_HINTS.stream().anyMatch(text::contains);
    }

    public boolean isCreatorQuestion(String message) {
        String text = safe(message).toLowerCase();
        if (text.isBlank()) {
            return false;
        }
        return CREATOR_QUESTION_HINTS.stream().anyMatch(text::contains);
    }

    public String resolveRole(String username) {
        if (!StringUtils.hasText(username)) {
            return "GUEST";
        }
        String value = username.trim();
        String lower = value.toLowerCase();
        if (lower.startsWith("guest_") || lower.startsWith("guest-") || value.startsWith("游客")) {
            return "GUEST";
        }
        if (IPV4_PATTERN.matcher(value).matches() || value.contains(":")) {
            return "GUEST";
        }
        return "USER";
    }
}
