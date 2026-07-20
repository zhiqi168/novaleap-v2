package com.novaleap.api.controller;

import com.novaleap.api.module.ai.agent.ReActAgentService;
import com.novaleap.api.module.ai.agent.dto.AgentChatRequest;
import com.novaleap.api.common.Result;
import com.novaleap.api.module.ai.assembler.AiViewAssembler;
import com.novaleap.api.module.ai.dto.AiCoachChatRequest;
import com.novaleap.api.module.ai.dto.AiNoteSummaryRequest;
import com.novaleap.api.module.ai.dto.AiResumeAnalyzeRequest;
import com.novaleap.api.module.ai.vo.AiCoachHistoryItemVO;
import com.novaleap.api.module.ai.vo.AiCoachSessionVO;
import com.novaleap.api.entity.User;
import com.novaleap.api.module.system.security.CurrentUser;
import com.novaleap.api.module.system.security.CurrentUserService;
import com.novaleap.api.module.system.web.ClientRequestService;
import com.novaleap.api.service.AiService;
import com.novaleap.api.service.QuestionAccessSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final String DEFAULT_TOPIC = "通用技术面试";
    private static final String DEFAULT_MODE = "coach";
    private static final String DEFAULT_RESUME_ROLE = "Java Backend Engineer";

    private final AiService aiService;
    private final QuestionAccessSupport questionAccessSupport;
    private final CurrentUserService currentUserService;
    private final ClientRequestService clientRequestService;
    private final ReActAgentService reactAgentService;

    public AiController(
            AiService aiService,
            QuestionAccessSupport questionAccessSupport,
            CurrentUserService currentUserService,
            ClientRequestService clientRequestService,
            ReActAgentService reactAgentService
    ) {
        this.aiService = aiService;
        this.questionAccessSupport = questionAccessSupport;
        this.currentUserService = currentUserService;
        this.clientRequestService = clientRequestService;
        this.reactAgentService = reactAgentService;
    }

    @GetMapping("/question/{id}/explain")
    public SseEmitter explainQuestion(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        String identifier = resolveIdentifier(authentication, request);
        if (questionAccessSupport.resolveAccessibleQuestion(id, authentication) == null) {
            return aiService.explainQuestion(identifier, -1L);
        }
        return aiService.explainQuestion(identifier, id);
    }

    @PostMapping("/coach/chat")
    public SseEmitter coachChat(
            Authentication authentication,
            HttpServletRequest request,
            @RequestBody @Valid AiCoachChatRequest payload
    ) {
        String message = safe(payload.getMessage());
        String topic = hasText(payload.getTopic()) ? payload.getTopic().trim() : DEFAULT_TOPIC;
        String mode = hasText(payload.getMode()) ? payload.getMode().trim() : DEFAULT_MODE;
        String identifier = resolveIdentifier(authentication, request);
        return aiService.coachChat(identifier, message, topic, payload.getImage(), mode);
    }

    @GetMapping("/coach/history")
    public Result<List<AiCoachHistoryItemVO>> coachHistory(
            Authentication authentication,
            @RequestParam(value = "limit", defaultValue = "40") Integer limit
    ) {
        String username = currentUserService.requireUsername(authentication);
        return Result.success(AiViewAssembler.toCoachHistory(aiService.getCoachHistory(username, limit == null ? 40 : limit)));
    }

    @GetMapping("/quote/daily")
    public Result<String> dailyQuote(Authentication authentication, HttpServletRequest request) {
        String identifier = resolveIdentifier(authentication, request);
        User user = currentUserService.loadDatabaseUser(authentication);
        String nickname = user == null ? "" : safe(user.getNickname());
        return Result.success(aiService.generateDailyQuote(identifier, nickname));
    }

    @PostMapping("/coach/session/new")
    public Result<AiCoachSessionVO> createCoachSession(Authentication authentication) {
        String username = currentUserService.requireUsername(authentication);
        return Result.success(new AiCoachSessionVO(aiService.createCoachSession(username)));
    }

    @DeleteMapping("/coach/history")
    public Result<Void> clearCoachHistory(Authentication authentication) {
        String username = currentUserService.requireUsername(authentication);
        aiService.clearCoachHistory(username);
        return Result.success();
    }

    @PostMapping("/resume/analyze")
    public SseEmitter analyzeResume(
            Authentication authentication,
            HttpServletRequest request,
            @RequestBody @Valid AiResumeAnalyzeRequest payload
    ) {
        String targetRole = hasText(payload.getTargetRole()) ? payload.getTargetRole().trim() : DEFAULT_RESUME_ROLE;
        String identifier = resolveIdentifier(authentication, request);
        return aiService.analyzeResume(identifier, payload.getResumeText(), targetRole);
    }

    @PostMapping("/notes/summarize")
    public Result<List<String>> summarizeNote(
            Authentication authentication,
            HttpServletRequest request,
            @RequestBody @Valid AiNoteSummaryRequest payload
    ) {
        String identifier = resolveIdentifier(authentication, request);
        return Result.success(aiService.summarizeNote(identifier, safe(payload.getTitle()), payload.getContent()));
    }

    @PostMapping("/agent/chat")
    public Result<ReActAgentService.AgentResult> agentChat(
            Authentication authentication,
            HttpServletRequest request,
            @RequestBody @Valid AgentChatRequest payload
    ) {
        String identifier = resolveIdentifier(authentication, request);
        ReActAgentService.AgentResult result = reactAgentService.execute(identifier, payload);
        return Result.success(result);
    }

    private String resolveIdentifier(Authentication authentication, HttpServletRequest request) {
        CurrentUser currentUser = currentUserService.current(authentication);
        if (currentUser.isAuthenticated()) {
            return currentUser.safeUsername();
        }
        String clientIp = clientRequestService.resolveClientIp(request);
        return hasText(clientIp) ? clientIp : "unknown";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
