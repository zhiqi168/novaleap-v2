package com.novaleap.api.module.questionbank.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novaleap.api.common.exception.ForbiddenException;
import com.novaleap.api.common.exception.NotFoundException;
import com.novaleap.api.entity.CustomQuestionBank;
import com.novaleap.api.entity.User;
import com.novaleap.api.mapper.CustomQuestionBankMapper;
import com.novaleap.api.module.question.support.QuestionReadCacheSupport;
import com.novaleap.api.module.questionbank.assembler.QuestionBankViewAssembler;
import com.novaleap.api.module.questionbank.dto.QuestionBankRenameRequest;
import com.novaleap.api.module.questionbank.support.QuestionBankSupport;
import com.novaleap.api.module.questionbank.vo.CustomQuestionBankVO;
import com.novaleap.api.module.system.security.CurrentUserService;
import com.novaleap.api.service.QuestionAccessSupport;
import com.novaleap.api.service.QuestionBankImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class QuestionBankApplicationService {

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final Logger log = LoggerFactory.getLogger(QuestionBankApplicationService.class);

    private final CustomQuestionBankMapper customQuestionBankMapper;
    private final QuestionBankImportService questionBankImportService;
    private final QuestionAccessSupport questionAccessSupport;
    private final QuestionBankSupport questionBankSupport;
    private final CurrentUserService currentUserService;
    private final QuestionReadCacheSupport questionReadCacheSupport;

    public QuestionBankApplicationService(
            CustomQuestionBankMapper customQuestionBankMapper,
            QuestionBankImportService questionBankImportService,
            QuestionAccessSupport questionAccessSupport,
            QuestionBankSupport questionBankSupport,
            CurrentUserService currentUserService,
            QuestionReadCacheSupport questionReadCacheSupport
    ) {
        this.customQuestionBankMapper = customQuestionBankMapper;
        this.questionBankImportService = questionBankImportService;
        this.questionAccessSupport = questionAccessSupport;
        this.questionBankSupport = questionBankSupport;
        this.currentUserService = currentUserService;
        this.questionReadCacheSupport = questionReadCacheSupport;
    }

    public Page<CustomQuestionBankVO> getMyBanks(Authentication authentication, Integer page, Integer size, Integer status) {
        long startNs = System.nanoTime();
        User currentUser = currentUserService.requireDatabaseUser(authentication, "\u8bf7\u5148\u767b\u5f55\u540e\u518d\u67e5\u770b\u6211\u7684\u9898\u5e93");
        String cacheKey = questionReadCacheSupport.myBanksKey(currentUser.getId(), page, size, status);
        Page<CustomQuestionBankVO> cached = questionReadCacheSupport.readMyBanksPage(cacheKey);
        if (cached != null) {
            log.info("[perf][question-bank-mine] cache-hit userId={} page={} size={} status={} records={} total={} tookMs={}",
                    currentUser.getId(), safe(page, 1), safe(size, 30), status,
                    cached.getRecords() == null ? 0 : cached.getRecords().size(), cached.getTotal(), elapsedMs(startNs));
            return cached;
        }

        Page<CustomQuestionBank> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CustomQuestionBank> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomQuestionBank::getUserId, currentUser.getId());

        Integer normalizedStatus = questionBankSupport.normalizeBankStatus(status);
        if (normalizedStatus != null) {
            wrapper.eq(CustomQuestionBank::getStatus, normalizedStatus);
        }
        wrapper.orderByDesc(CustomQuestionBank::getUpdatedAt).orderByDesc(CustomQuestionBank::getCreatedAt);

        Page<CustomQuestionBank> result = customQuestionBankMapper.selectPage(pageParam, wrapper);
        Page<CustomQuestionBankVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(bank -> QuestionBankViewAssembler.toUserVO(bank, questionBankSupport))
                .toList());
        questionReadCacheSupport.writeMyBanksPage(cacheKey, voPage);
        log.info("[perf][question-bank-mine] cache-miss userId={} page={} size={} status={} records={} total={} tookMs={}",
                currentUser.getId(), safe(page, 1), safe(size, 30), status,
                voPage.getRecords() == null ? 0 : voPage.getRecords().size(), voPage.getTotal(), elapsedMs(startNs));
        return voPage;
    }

    public CustomQuestionBankVO importQuestionBank(
            Authentication authentication,
            MultipartFile file,
            String name,
            String category,
            Integer difficulty
    ) throws IOException {
        User currentUser = currentUserService.requireDatabaseUser(authentication, "\u8bf7\u5148\u767b\u5f55\u540e\u518d\u5bfc\u5165\u9898\u5e93");
        questionBankImportService.validateImportFile(file, MAX_FILE_SIZE);

        QuestionBankImportService.ImportPayload payload = questionBankImportService.analyzeFile(file.getOriginalFilename(), file.getBytes());
        if (payload.questions().isEmpty()) {
            throw new IllegalArgumentException("\u672a\u89e3\u6790\u5230\u6709\u6548\u9898\u76ee\uff0c\u8bf7\u68c0\u67e5 TXT \u683c\u5f0f");
        }

        String normalizedCategory = questionBankSupport.normalizeCategory(category);
        Integer normalizedDifficulty = questionBankSupport.normalizeDifficulty(difficulty);

        CustomQuestionBank bank = new CustomQuestionBank();
        bank.setUserId(currentUser.getId());
        bank.setName(questionBankSupport.resolveBankName(name, file.getOriginalFilename()));
        bank.setOriginalFileName(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : bank.getName());
        bank.setFileType(payload.fileType());
        bank.setRawContent(payload.rawText());
        bank.setCategory(normalizedCategory == null ? "java" : normalizedCategory);
        bank.setDifficulty(normalizedDifficulty == null ? 2 : normalizedDifficulty);
        bank.setStatus(QuestionAccessSupport.BANK_STATUS_PENDING);
        bank.setQuestionCount(payload.questions().size());
        bank.setImportedQuestionCount(0);
        bank.setRejectReason(null);
        bank.setAuditedAt(null);
        bank.setImportedAt(null);
        LocalDateTime now = LocalDateTime.now();
        bank.setCreatedAt(now);
        bank.setUpdatedAt(now);
        customQuestionBankMapper.insert(bank);
        questionReadCacheSupport.evictMyBanks(currentUser.getId());

        return QuestionBankViewAssembler.toUserVO(bank, questionBankSupport);
    }

    public CustomQuestionBankVO renameQuestionBank(Long id, QuestionBankRenameRequest body, Authentication authentication) {
        User currentUser = currentUserService.requireDatabaseUser(authentication, "\u8bf7\u5148\u767b\u5f55\u540e\u518d\u91cd\u547d\u540d\u9898\u5e93");

        CustomQuestionBank bank = customQuestionBankMapper.selectById(id);
        if (bank == null) {
            throw new NotFoundException("\u9898\u5e93\u4e0d\u5b58\u5728");
        }
        if (!Objects.equals(currentUser.getId(), bank.getUserId()) && !questionAccessSupport.isAdmin(authentication)) {
            throw new ForbiddenException("\u4f60\u6ca1\u6709\u64cd\u4f5c\u8fd9\u4e2a\u9898\u5e93\u7684\u6743\u9650");
        }

        bank.setName(questionBankSupport.limitLength(body.getName().trim(), 120));
        bank.setUpdatedAt(LocalDateTime.now());
        customQuestionBankMapper.updateById(bank);
        questionReadCacheSupport.evictMyBanks(bank.getUserId());
        return QuestionBankViewAssembler.toUserVO(bank, questionBankSupport);
    }

    private int safe(Integer value, int defaultValue) {
        return value == null || value < 1 ? defaultValue : value;
    }

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }
}
