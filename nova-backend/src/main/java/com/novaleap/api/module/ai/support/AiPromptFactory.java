package com.novaleap.api.module.ai.support;

import com.novaleap.api.entity.Question;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiPromptFactory {

    private static final String COACH_SYSTEM_PROMPT = """
            You are NovaLeap Coach, the self-developed assistant of NovaLeap.
            Respond in Simplified Chinese unless the user explicitly asks for another language.
            Be practical, supportive, and direct.
            For identity questions, you must clearly state: "I am the self-developed model of NovaLeap."
            Never claim to be OpenAI, ChatGPT, GPT, Claude, or any third-party company/model.
            If asked about the creator, clearly say: "Zhang Zhiqi is my creator."
            """;

    public String coachSystemPrompt() {
        return COACH_SYSTEM_PROMPT;
    }

    public String buildQuestionExplainPrompt(Question question) {
        return String.format(
                """
                Please explain the following interview question in Simplified Chinese.
                Output in Markdown and include:
                1. Core knowledge points
                2. Recommended solution approach
                3. Answer explanation
                4. Common pitfalls
                5. Follow-up questions

                Title:
                %s

                Description:
                %s

                Reference answer:
                %s
                """,
                safe(question == null ? null : question.getTitle()),
                safe(question == null ? null : question.getContent()),
                safe(question == null ? null : question.getStandardAnswer())
        );
    }

    public String buildIdentityPrompt(String userQuestion) {
        return """
                The user is asking about your identity.
                Reply in Simplified Chinese.
                You must explicitly include the sentence:
                "I am the self-developed model of NovaLeap."
                You may add one or two short sentences about your capabilities.
                Do not mention any third-party company or model.
                User question:
                %s
                """.formatted(safe(userQuestion));
    }

    public String buildCoachPrompt(String mode, String topic, String historyText, String message, String image, String externalContext) {
        return String.format(
                """
                Reply in Simplified Chinese.
                Current mode: %s
                Topic: %s

                Recent conversation:
                %s

                User question:
                %s

                Image note:
                %s

                External context:
                %s
                """,
                safe(mode),
                safe(topic),
                StringUtils.hasText(historyText) ? historyText : "(none)",
                safe(message),
                StringUtils.hasText(image) ? "User attached an image. If you cannot inspect it, say so briefly and continue with the text context." : "(none)",
                StringUtils.hasText(externalContext) ? externalContext : "(none)"
        );
    }

    public String buildResumePrompt(String targetRole, String defaultRole, String resumeText) {
        return String.format(
                """
                You are a senior resume reviewer for large tech companies.
                Reply in Simplified Chinese.
                First give concise analysis suggestions.
                Then output the separator line exactly as:
                ---RESUME_CONTENT_START---
                Then provide a complete optimized resume in Markdown.
                Keep all original project experience, make it more measurable, and improve keyword matching.

                Target role:
                %s

                Resume content:
                %s
                """,
                safe(targetRole).isBlank() ? safe(defaultRole) : safe(targetRole),
                safe(resumeText)
        );
    }

    public String buildNoteSummaryPrompt(String title, String noteContent) {
        return String.format(
                """
                Read the following note and output only a JSON array.
                Requirements:
                - reply in Simplified Chinese
                - exactly 3 summary points
                - each point should be concise and actionable

                Title:
                %s

                Content:
                %s
                """,
                safe(title).isBlank() ? "Untitled Note" : safe(title),
                safe(noteContent)
        );
    }

    public String buildNoteModerationPrompt(String title, String noteContent) {
        return String.format(
                """
                You are a content moderation assistant.
                Reply with JSON only, using this shape:
                {"approved":true,"reason":"..."}
                The reason must be short and in Simplified Chinese.

                Title:
                %s

                Content:
                %s
                """,
                safe(title).isBlank() ? "Untitled Note" : safe(title),
                safe(noteContent)
        );
    }

    public String buildWishEmotionPrompt(String content) {
        return """
                I have a wish related to growth in technology.
                Reply with only one emotion label from this list:
                happy, hopeful, confused, anxious
                Do not add punctuation or extra explanation.
                Wish content:
                %s
                """.formatted(safe(content));
    }

    public String buildDailyQuotePrompt(String nickname) {
        return """
                Write one daily motivational quote in Simplified Chinese for NovaLeap users.
                Requirements:
                - output only the quote itself
                - 16 to 28 Chinese characters preferred
                - warm, grounded, encouraging
                - no markdown, no quotes, no numbering, no emoji
                - avoid being too generic or preachy

                User nickname for light personalization:
                %s
                """.formatted(safe(nickname).isBlank() ? "学习者" : safe(nickname));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
