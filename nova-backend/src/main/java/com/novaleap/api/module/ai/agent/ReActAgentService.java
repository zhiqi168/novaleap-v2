package com.novaleap.api.module.ai.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novaleap.api.entity.Note;
import com.novaleap.api.entity.Question;
import com.novaleap.api.mapper.NoteMapper;
import com.novaleap.api.mapper.QuestionMapper;
import com.novaleap.api.module.ai.agent.dto.AgentChatRequest;
import com.novaleap.api.module.ai.support.AiCoachSessionSupport;
import com.novaleap.api.module.ai.support.AiExternalContextService;
import com.novaleap.api.module.ai.support.AiModelGateway;
import com.novaleap.api.service.AiLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent 服务 —— 先用 AI 自己的知识回答，再在后台搜相关题库/笔记以卡片补充。
 */
@Service
public class ReActAgentService {

    private static final String FALLBACK_MSG = "抱歉，AI 服务暂时不可用，请稍后重试。";

    private static final Pattern ANSWER_PATTERN = Pattern.compile(
            "ANSWER:\\s*(.*)", Pattern.DOTALL
    );

    private static final Logger log = LoggerFactory.getLogger(ReActAgentService.class);

    private final ReActPromptFactory promptFactory;
    private final AiModelGateway aiModelGateway;
    private final AiExternalContextService externalContextService;
    private final QuestionMapper questionMapper;
    private final NoteMapper noteMapper;
    private final AiCoachSessionSupport coachSessionSupport;

    public ReActAgentService(
            ReActPromptFactory promptFactory,
            AiModelGateway aiModelGateway,
            AiExternalContextService externalContextService,
            QuestionMapper questionMapper,
            NoteMapper noteMapper,
            AiCoachSessionSupport coachSessionSupport
    ) {
        this.promptFactory = promptFactory;
        this.aiModelGateway = aiModelGateway;
        this.externalContextService = externalContextService;
        this.questionMapper = questionMapper;
        this.noteMapper = noteMapper;
        this.coachSessionSupport = coachSessionSupport;
    }

    /**
     * 执行 Agent 对话。
     * 流程：先让 AI 用自己的知识回答 → 后台搜索相关题库/笔记 → 以卡片展示。
     * 不再使用 ReAct 循环，避免工具调用失败导致"AI不可用"。
     */
    public AgentResult execute(String username, AgentChatRequest request) {
        AgentResult result = new AgentResult();
        String userMessage = request.getMessage();

        if (!StringUtils.hasText(userMessage)) {
            result.setAnswer("请输入你想交流的内容。");
            return result;
        }

        // 保存用户消息到历史记录
        coachSessionSupport.saveCoachMessage(username, "user", userMessage, "agent", "通用技术面试");

        try {
            // 1. 构建系统提示词，注入外部上下文（天气、联网搜索等）
            String systemPrompt = promptFactory.buildSystemPrompt();
            String externalContext = externalContextService.buildExternalContext(userMessage);
            if (!externalContext.isBlank()) {
                systemPrompt += "\n\n当前实时信息：\n" + externalContext;
            }
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage(userMessage));

            String response = aiModelGateway.callChatWithMessages(messages, AiLimitService.AiModule.COACH, null, 0);
            if (!StringUtils.hasText(response)) {
                result.setAnswer(FALLBACK_MSG);
                coachSessionSupport.saveAgentMessage(username, "assistant", result.getAnswer(), "agent", "通用技术面试", result.getQuestions(), result.getNotes());
                return result;
            }

            // 2. 直接使用模型完整响应，不再截取 ANSWER: 后面的内容
            result.setAnswer(response.trim());

            // 3. 后台搜索相关题库和笔记作为卡片补充
            enrichQuestionRefs(result, userMessage);

            // 4. 保存到历史记录并返回
            coachSessionSupport.saveAgentMessage(username, "assistant", result.getAnswer(), "agent", "通用技术面试", result.getQuestions(), result.getNotes());
            return result;

        } catch (Exception e) {
            log.error("Agent execute failed: {}", e.getMessage(), e);
            result.setAnswer("处理请求时出现内部错误，请稍后重试。");
            return result;
        }
    }

    /**
     * 从模型回复中提取最终回答。
     */
    private String extractAnswer(String response) {
        if (response == null) return null;

        // 从后往前找最后一个 ANSWER:
        int lastIdx = response.lastIndexOf("ANSWER:");
        if (lastIdx < 0) return null;

        // 如果 ANSWER 后面还有 ACTION，说明不是最终回答
        String afterAnswer = response.substring(lastIdx);
        if (afterAnswer.contains("ACTION:")) return null;

        Matcher matcher = ANSWER_PATTERN.matcher(afterAnswer);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 根据用户消息搜索相关题库和笔记，以卡片形式展示。
     * 这是一个后台补充步骤，不影响 AI 的回答内容。
     */
    private void enrichQuestionRefs(AgentResult result, String userMessage) {
        if (!StringUtils.hasText(userMessage)) return;

        // 从用户消息中提取关键词
        String keyword = extractSearchKeyword(userMessage);
        if (keyword.isBlank()) return;

        // 搜索题库
        searchQuestionsAndAddToResult(result, keyword);

        // 搜索笔记（用提取的关键词 + 完整消息双保险，搜所有公开手记）
        searchNotesAndAddToResult(result, keyword);
        // 如果提取的关键词没搜到笔记，再用完整消息前15字再搜一次
        if (result.getNotes().isEmpty()) {
            String fullKeyword = userMessage.length() > 15 ? userMessage.substring(0, 15) : userMessage;
            if (!fullKeyword.equals(keyword)) {
                searchNotesAndAddToResult(result, fullKeyword);
            }
        }
    }

    /**
     * 从用户消息中提取搜索关键词。优先从搜题意图词后提取，失败则提取技术关键词，最后用完整消息。
     */
    private String extractSearchKeyword(String message) {
        if (message == null || message.isBlank()) return "";
        String[] searchHints = {"找", "搜", "推荐", "学习", "看", "查", "练", "题目", "题"};
        String lower = message.toLowerCase();
        for (String hint : searchHints) {
            int idx = lower.indexOf(hint);
            if (idx >= 0) {
                int start = idx + hint.length();
                if (start < message.length()) {
                    String after = message.substring(start).trim();
                    after = after.replaceAll("^[几一\\d]+[道个些点下条]?", "").trim();
                    if (after.length() > 20) after = after.substring(0, 20);
                    after = after.replaceAll("[的了吗是吧呢么啊哈哦哟嗯]+$", "").trim();
                    // 去掉末尾的非技术内容，如 "java的题" → "java"
                    after = after.replaceAll("[的题问题目方法技术面试]+$", "").trim();
                    if (!after.isBlank()) return after;
                }
            }
        }
        // 提取已知技术关键词
        String[] techKeywords = {"java", "spring", "并发", "线程", "jvm", "mysql", "redis", "算法", "网络", "linux", "系统设计", "微服务", "分布式", "缓存", "消息队列", "索引"};
        for (String kw : techKeywords) {
            if (lower.contains(kw)) {
                return kw;
            }
        }
        // 没有技术关键词时，用完整消息做模糊搜索
        return message.length() > 15 ? message.substring(0, 15) : message;
    }

    private void searchQuestionsAndAddToResult(AgentResult result, String keyword) {
        try {
            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Question::getStatus, 1);
            wrapper.like(Question::getTitle, keyword);
            wrapper.orderByDesc(Question::getViewCount);
            wrapper.last("LIMIT 6");

            List<Question> questions = questionMapper.selectList(wrapper);

            // 标题搜不到时尝试搜 content 字段
            if (questions.isEmpty()) {
                LambdaQueryWrapper<Question> contentWrapper = new LambdaQueryWrapper<>();
                contentWrapper.eq(Question::getStatus, 1);
                contentWrapper.like(Question::getContent, keyword);
                contentWrapper.orderByDesc(Question::getViewCount);
                contentWrapper.last("LIMIT 6");
                questions = questionMapper.selectList(contentWrapper);
            }

            for (Question q : questions) {
                result.getQuestions().add(new AgentResult.QuestionRef(
                        q.getId(),
                        q.getTitle() != null ? q.getTitle().trim() : "",
                        q.getCategory() != null ? q.getCategory() : "",
                        q.getDifficulty() != null ? q.getDifficulty() : 0
                ));
            }
        } catch (Exception e) {
            log.debug("search questions for refs failed: {}", e.getMessage());
        }
    }

    private void searchNotesAndAddToResult(AgentResult result, String keyword) {
        if (keyword.isBlank()) return;
        // 搜索所有已审核公开的手记，不仅是用户自己的
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getStatus, 1); // 只搜已审核的公开手记
        wrapper.and(w -> w.like(Note::getTitle, keyword).or().like(Note::getContent, keyword));
        wrapper.orderByDesc(Note::getCreatedAt);
        wrapper.last("LIMIT 5");

        List<Note> notes = noteMapper.selectList(wrapper);
        for (Note n : notes) {
            result.getNotes().add(new AgentResult.NoteRef(
                    n.getId(),
                    n.getTitle() != null ? n.getTitle().trim() : "",
                    n.getSummary() != null ? n.getSummary().trim() : ""
            ));
        }
    }

    /**
     * Agent 执行结果。
     */
    public static class AgentResult {
        private String answer;
        private final java.util.List<Step> steps = new java.util.ArrayList<>();
        private final java.util.List<QuestionRef> questions = new java.util.ArrayList<>();
        private final java.util.List<NoteRef> notes = new java.util.ArrayList<>();

        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public java.util.List<Step> getSteps() { return steps; }
        public java.util.List<QuestionRef> getQuestions() { return questions; }
        public java.util.List<NoteRef> getNotes() { return notes; }

        public void addStep(String type, String content) {
            steps.add(new Step(type, content));
        }

        public static class Step {
            private final String type;
            private final String content;

            public Step(String type, String content) {
                this.type = type;
                this.content = content;
            }

            public String getType() { return type; }
            public String getContent() { return content; }
        }

        /**
         * 结构化题目引用，前端可渲染为可点击卡片。
         */
        public static class QuestionRef {
            private final Long id;
            private final String title;
            private final String category;
            private final int difficulty;

            public QuestionRef(Long id, String title, String category, int difficulty) {
                this.id = id;
                this.title = title;
                this.category = category;
                this.difficulty = difficulty;
            }

            public Long getId() { return id; }
            public String getTitle() { return title; }
            public String getCategory() { return category; }
            public int getDifficulty() { return difficulty; }
        }

        /**
         * 结构化笔记引用，前端可渲染为可点击卡片。
         */
        public static class NoteRef {
            private final Long id;
            private final String title;
            private final String summary;

            public NoteRef(Long id, String title, String summary) {
                this.id = id;
                this.title = title;
                this.summary = summary;
            }

            public Long getId() { return id; }
            public String getTitle() { return title; }
            public String getSummary() { return summary; }
        }
    }
}