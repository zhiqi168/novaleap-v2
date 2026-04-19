package com.novaleap.api.service.impl;

import com.novaleap.api.entity.Question;
import com.novaleap.api.mapper.QuestionMapper;
import com.novaleap.api.module.ai.support.AiCoachSessionSupport;
import com.novaleap.api.module.ai.support.AiExternalContextService;
import com.novaleap.api.module.ai.support.AiIdentitySupport;
import com.novaleap.api.module.ai.support.AiModelGateway;
import com.novaleap.api.module.ai.support.AiNoteWorkflowSupport;
import com.novaleap.api.module.ai.support.AiPromptFactory;
import com.novaleap.api.service.AiLimitService;
import com.novaleap.api.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private static final String AI_KEY_PLACEHOLDER = "sk-disabled-placeholder";
    private static final String AI_UNAVAILABLE_MSG = "当前 AI 服务暂时不可用，请稍后重试。";
    private static final String QUESTION_NOT_FOUND_MSG = "题目不存在或无权访问。";
    private static final String QUESTION_OFFLINE_MSG = "题目不存在或未上架。";
    private static final String EMPTY_CHAT_MSG = "请输入想交流的内容。";
    private static final String EMPTY_RESUME_MSG = "简历内容为空，请先输入简历文本。";
    private static final String SOLVER_FALLBACK_MSG = "当前无法生成题解，请稍后重试。";
    private static final String COACH_FALLBACK_MSG = "当前无法生成回复，请稍后重试。";
    private static final String RESUME_FALLBACK_MSG = "当前无法完成简历分析，请稍后重试。";
    private static final String DAILY_QUOTE_FALLBACK = "把今天认真走稳，明天就会向你靠近。";
    private static final String NOVALEAP_IDENTITY_LINE = "我是NovaLeap自主研发的模型。";
    private static final String NOVALEAP_IDENTITY_FALLBACK = "我仍可以继续提供面试辅导、技术讲解以及职业发展建议。";
    private static final String CREATOR_FIXED_REPLY = "张志琪是我的创作者。";
    private static final String DEFAULT_TOPIC = "通用技术面试";
    private static final String DEFAULT_MODE = "coach";
    private static final String DEFAULT_RESUME_ROLE = "Java后端工程师";
    private static final int MAX_PROMPT_HISTORY = 8;

    private final QuestionMapper questionMapper;
    private final AiLimitService aiLimitService;
    private final AiCoachSessionSupport aiCoachSessionSupport;
    private final AiExternalContextService aiExternalContextService;
    private final AiModelGateway aiModelGateway;
    private final AiPromptFactory aiPromptFactory;
    private final AiIdentitySupport aiIdentitySupport;
    private final AiNoteWorkflowSupport aiNoteWorkflowSupport;
    private final String openAiApiKey;

    public AiServiceImpl(
            QuestionMapper questionMapper,
            AiLimitService aiLimitService,
            AiCoachSessionSupport aiCoachSessionSupport,
            AiExternalContextService aiExternalContextService,
            AiModelGateway aiModelGateway,
            AiPromptFactory aiPromptFactory,
            AiIdentitySupport aiIdentitySupport,
            AiNoteWorkflowSupport aiNoteWorkflowSupport,
            @Value("${spring.ai.openai.api-key:" + AI_KEY_PLACEHOLDER + "}") String openAiApiKey
    ) {
        this.questionMapper = questionMapper;
        this.aiLimitService = aiLimitService;
        this.aiCoachSessionSupport = aiCoachSessionSupport;
        this.aiExternalContextService = aiExternalContextService;
        this.aiModelGateway = aiModelGateway;
        this.aiPromptFactory = aiPromptFactory;
        this.aiIdentitySupport = aiIdentitySupport;
        this.aiNoteWorkflowSupport = aiNoteWorkflowSupport;
        this.openAiApiKey = openAiApiKey == null ? "" : openAiApiKey.trim();
        if (!hasAiCapability()) {
            log.warn("AI capability is disabled: missing SPRING_AI_OPENAI_API_KEY");
        }
    }

    @Override
    public SseEmitter explainQuestion(String username, Long questionId) {
        if (questionId == null || questionId <= 0) {
            return aiModelGateway.streamStaticAnswer(QUESTION_NOT_FOUND_MSG);
        }

        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            return aiModelGateway.streamStaticAnswer(QUESTION_NOT_FOUND_MSG);
        }
        if (!Integer.valueOf(1).equals(question.getStatus())) {
            return aiModelGateway.streamStaticAnswer(QUESTION_OFFLINE_MSG);
        }
        if (!hasAiCapability()) {
            return aiModelGateway.streamStaticAnswer(AI_UNAVAILABLE_MSG);
        }

        AiLimitService.LimitCheckResult limit = aiLimitService.checkLimit(
                username,
                aiIdentitySupport.resolveRole(username),
                AiLimitService.AiModule.SOLVER
        );
        if (!limit.isAllowed()) {
            return aiModelGateway.streamStaticAnswer(limit.getMessage());
        }

        String prompt = aiPromptFactory.buildQuestionExplainPrompt(question);
        SseEmitter stream = aiModelGateway.streamModelAnswer("", prompt, SOLVER_FALLBACK_MSG, null, AiLimitService.AiModule.SOLVER);
        return stream != null ? stream : aiModelGateway.streamStaticAnswer(aiModelGateway.callModel("", prompt, SOLVER_FALLBACK_MSG, AiLimitService.AiModule.SOLVER));
    }

    @Override
    public SseEmitter coachChat(String username, String message, String topic, String image, String mode) {
        String cleanMessage = aiIdentitySupport.safe(message);
        if (cleanMessage.isBlank()) {
            return aiModelGateway.streamStaticAnswer(EMPTY_CHAT_MSG);
        }

        String finalTopic = aiIdentitySupport.safe(topic).isBlank() ? DEFAULT_TOPIC : aiIdentitySupport.safe(topic);
        String finalMode = aiIdentitySupport.safe(mode).isBlank() ? DEFAULT_MODE : aiIdentitySupport.safe(mode);
        String role = aiIdentitySupport.resolveRole(username);

        if (!hasAiCapability()) {
            aiCoachSessionSupport.saveCoachMessage(username, "user", cleanMessage, finalMode, finalTopic);
            aiCoachSessionSupport.saveCoachMessage(username, "assistant", AI_UNAVAILABLE_MSG, finalMode, finalTopic);
            return aiModelGateway.streamStaticAnswer(AI_UNAVAILABLE_MSG);
        }

        AiLimitService.LimitCheckResult limit = aiLimitService.checkLimit(username, role, AiLimitService.AiModule.COACH);
        if (!limit.isAllowed()) {
            return aiModelGateway.streamStaticAnswer(limit.getMessage());
        }

        if (aiIdentitySupport.isCreatorQuestion(cleanMessage)) {
            aiCoachSessionSupport.saveCoachMessage(username, "user", cleanMessage, finalMode, finalTopic);
            aiCoachSessionSupport.saveCoachMessage(username, "assistant", CREATOR_FIXED_REPLY, finalMode, finalTopic);
            return aiModelGateway.streamStaticAnswer(CREATOR_FIXED_REPLY);
        }

        if (aiIdentitySupport.isIdentityQuestion(cleanMessage)) {
            String answer = aiModelGateway.callModel(
                    aiPromptFactory.coachSystemPrompt(),
                    aiPromptFactory.buildIdentityPrompt(cleanMessage),
                    NOVALEAP_IDENTITY_LINE + " " + NOVALEAP_IDENTITY_FALLBACK,
                    AiLimitService.AiModule.COACH
            );
            answer = answer.contains(NOVALEAP_IDENTITY_LINE) ? answer : NOVALEAP_IDENTITY_LINE + " " + answer;
            aiCoachSessionSupport.saveCoachMessage(username, "user", cleanMessage, finalMode, finalTopic);
            aiCoachSessionSupport.saveCoachMessage(username, "assistant", answer, finalMode, finalTopic);
            return aiModelGateway.streamStaticAnswer(answer);
        }

        List<Map<String, Object>> history = aiCoachSessionSupport.getCoachHistory(username, MAX_PROMPT_HISTORY);
        if (!history.isEmpty()) {
            Collections.reverse(history);
        }
        String historyText = aiCoachSessionSupport.buildHistoryPrompt(history, MAX_PROMPT_HISTORY);
        String externalContext = aiExternalContextService.buildExternalContext(cleanMessage);
        String prompt = aiPromptFactory.buildCoachPrompt(finalMode, finalTopic, historyText, cleanMessage, image, externalContext);

        aiCoachSessionSupport.saveCoachMessage(username, "user", cleanMessage, finalMode, finalTopic);
        SseEmitter stream = aiModelGateway.streamModelAnswer(
                aiPromptFactory.coachSystemPrompt(),
                prompt,
                COACH_FALLBACK_MSG,
                answer -> aiCoachSessionSupport.saveCoachMessage(username, "assistant", answer, finalMode, finalTopic),
                AiLimitService.AiModule.COACH
        );
        if (stream != null) {
            return stream;
        }

        String answer = aiModelGateway.callModel(aiPromptFactory.coachSystemPrompt(), prompt, COACH_FALLBACK_MSG, AiLimitService.AiModule.COACH);
        aiCoachSessionSupport.saveCoachMessage(username, "assistant", answer, finalMode, finalTopic);
        return aiModelGateway.streamStaticAnswer(answer);
    }

    @Override
    public List<Map<String, Object>> getCoachHistory(String username, int limit) {
        return aiCoachSessionSupport.getCoachHistory(username, limit);
    }

    @Override
    public String createCoachSession(String username) {
        return aiCoachSessionSupport.createCoachSession(username);
    }

    @Override
    public void clearCoachHistory(String username) {
        aiCoachSessionSupport.clearCoachHistory(username);
    }

    @Override
    public SseEmitter analyzeResume(String username, String resumeText, String targetRole) {
        String cleanResume = aiIdentitySupport.safe(resumeText);
        if (cleanResume.isBlank()) {
            return aiModelGateway.streamStaticAnswer(EMPTY_RESUME_MSG);
        }
        if (!hasAiCapability()) {
            return aiModelGateway.streamStaticAnswer(AI_UNAVAILABLE_MSG);
        }

        AiLimitService.LimitCheckResult limit = aiLimitService.checkLimit(
                username,
                aiIdentitySupport.resolveRole(username),
                AiLimitService.AiModule.RESUME
        );
        if (!limit.isAllowed()) {
            return aiModelGateway.streamStaticAnswer(limit.getMessage());
        }

        String prompt = aiPromptFactory.buildResumePrompt(targetRole, DEFAULT_RESUME_ROLE, cleanResume);
        SseEmitter stream = aiModelGateway.streamModelAnswer("", prompt, RESUME_FALLBACK_MSG, null, AiLimitService.AiModule.RESUME);
        return stream != null ? stream : aiModelGateway.streamStaticAnswer(aiModelGateway.callModel("", prompt, RESUME_FALLBACK_MSG, AiLimitService.AiModule.RESUME));
    }

    @Override
    public List<String> summarizeNote(String username, String title, String noteContent) {
        return aiNoteWorkflowSupport.summarizeNote(username, aiIdentitySupport.resolveRole(username), title, noteContent, hasAiCapability());
    }

    @Override
    public String generateDailyQuote(String username, String nickname) {
        if (!hasAiCapability()) {
            return DAILY_QUOTE_FALLBACK;
        }

        AiLimitService.LimitCheckResult limit = aiLimitService.checkLimit(
                username,
                aiIdentitySupport.resolveRole(username),
                AiLimitService.AiModule.CHAT
        );
        if (!limit.isAllowed()) {
            return DAILY_QUOTE_FALLBACK;
        }

        String answer = aiModelGateway.callModel(
                aiPromptFactory.coachSystemPrompt(),
                aiPromptFactory.buildDailyQuotePrompt(nickname),
                DAILY_QUOTE_FALLBACK,
                AiLimitService.AiModule.CHAT
        );
        String sanitized = aiIdentitySupport.safe(answer)
                .replace("\r", "")
                .replace("\n", "")
                .replace("\"", "")
                .replace("“", "")
                .replace("”", "");
        return sanitized.isBlank() ? DAILY_QUOTE_FALLBACK : sanitized;
    }

    @Override
    public NoteModerationResult moderateNote(String title, String noteContent) {
        return aiNoteWorkflowSupport.moderateNote(title, noteContent, hasAiCapability());
    }

    private boolean hasAiCapability() {
        return !openAiApiKey.isBlank() && !AI_KEY_PLACEHOLDER.equalsIgnoreCase(openAiApiKey);
    }
}
