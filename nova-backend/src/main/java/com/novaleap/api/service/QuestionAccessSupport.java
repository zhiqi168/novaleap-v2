package com.novaleap.api.service;

import com.novaleap.api.entity.CustomQuestionBank;
import com.novaleap.api.entity.Question;
import com.novaleap.api.entity.User;
import com.novaleap.api.mapper.CustomQuestionBankMapper;
import com.novaleap.api.mapper.QuestionMapper;
import com.novaleap.api.module.system.security.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.Objects;

@Service
public class QuestionAccessSupport {

    public static final String SOURCE_OFFICIAL = "OFFICIAL";
    public static final String SOURCE_CUSTOM = "CUSTOM";
    public static final int BANK_STATUS_PENDING = 0;
    public static final int BANK_STATUS_APPROVED = 1;
    public static final int BANK_STATUS_REJECTED = 2;

    private final QuestionMapper questionMapper;
    private final CustomQuestionBankMapper customQuestionBankMapper;
    private final CurrentUserService currentUserService;

    public QuestionAccessSupport(
            QuestionMapper questionMapper,
            CustomQuestionBankMapper customQuestionBankMapper,
            CurrentUserService currentUserService
    ) {
        this.questionMapper = questionMapper;
        this.customQuestionBankMapper = customQuestionBankMapper;
        this.currentUserService = currentUserService;
    }

    public boolean isAnonymous(Authentication authentication) {
        return currentUserService.isAnonymous(authentication);
    }

    public boolean isAdmin(Authentication authentication) {
        return currentUserService.isAdmin(authentication);
    }

    public boolean isGuest(Authentication authentication) {
        return currentUserService.isGuest(authentication);
    }

    public User resolveCurrentUser(Authentication authentication) {
        return currentUserService.loadDatabaseUser(authentication);
    }

    public Long resolveCurrentUserId(Authentication authentication) {
        User currentUser = resolveCurrentUser(authentication);
        return currentUser == null ? null : currentUser.getId();
    }

    public CustomQuestionBank resolveAccessibleBank(Long bankId, Authentication authentication) {
        if (bankId == null) {
            return null;
        }
        CustomQuestionBank bank = customQuestionBankMapper.selectById(bankId);
        if (bank == null || bank.getStatus() == null || bank.getStatus() != BANK_STATUS_APPROVED) {
            return null;
        }
        if (isAdmin(authentication)) {
            return bank;
        }
        Long currentUserId = resolveCurrentUserId(authentication);
        if (currentUserId == null) {
            return null;
        }
        return Objects.equals(currentUserId, bank.getUserId()) ? bank : null;
    }

    public Question resolveAccessibleQuestion(Long questionId, Authentication authentication) {
        if (questionId == null) {
            return null;
        }
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                        Question::getId,
                        Question::getTitle,
                        Question::getContent,
                        Question::getStandardAnswer,
                        Question::getDifficulty,
                        Question::getCategory,
                        Question::getTags,
                        Question::getViewCount,
                        Question::getStatus,
                        Question::getSourceType,
                        Question::getCustomBankId,
                        Question::getOwnerUserId
                )
                .eq(Question::getId, questionId)
                .last("LIMIT 1");
        Question question = questionMapper.selectOne(wrapper);
        if (question == null || question.getStatus() == null || question.getStatus() != 1) {
            return null;
        }
        if (!SOURCE_CUSTOM.equalsIgnoreCase(safe(question.getSourceType()))) {
            return question;
        }
        if (question.getCustomBankId() == null) {
            return null;
        }
        return resolveAccessibleBank(question.getCustomBankId(), authentication) == null ? null : question;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
