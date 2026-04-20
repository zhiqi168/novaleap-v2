package com.novaleap.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novaleap.api.common.exception.NotFoundException;
import com.novaleap.api.entity.CustomQuestionBank;
import com.novaleap.api.entity.Question;
import com.novaleap.api.entity.User;
import com.novaleap.api.mapper.CustomQuestionBankMapper;
import com.novaleap.api.mapper.QuestionMapper;
import com.novaleap.api.mapper.UserMapper;
import com.novaleap.api.module.admin.questionbank.assembler.AdminQuestionBankViewAssembler;
import com.novaleap.api.module.admin.questionbank.dto.AdminQuestionBankAuditRequest;
import com.novaleap.api.module.admin.questionbank.vo.AdminOfficialQuestionImportVO;
import com.novaleap.api.module.admin.questionbank.vo.AdminQuestionBankVO;
import com.novaleap.api.module.question.support.QuestionReadCacheSupport;
import com.novaleap.api.module.questionbank.support.QuestionBankSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminQuestionBankAuditService {

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;

    private final CustomQuestionBankMapper customQuestionBankMapper;
    private final QuestionMapper questionMapper;
    private final UserMapper userMapper;
    private final QuestionBankImportService questionBankImportService;
    private final QuestionBankSupport questionBankSupport;
    private final QuestionReadCacheSupport questionReadCacheSupport;

    public AdminQuestionBankAuditService(
            CustomQuestionBankMapper customQuestionBankMapper,
            QuestionMapper questionMapper,
            UserMapper userMapper,
            QuestionBankImportService questionBankImportService,
            QuestionBankSupport questionBankSupport,
            QuestionReadCacheSupport questionReadCacheSupport
    ) {
        this.customQuestionBankMapper = customQuestionBankMapper;
        this.questionMapper = questionMapper;
        this.userMapper = userMapper;
        this.questionBankImportService = questionBankImportService;
        this.questionBankSupport = questionBankSupport;
        this.questionReadCacheSupport = questionReadCacheSupport;
    }

    public Page<AdminQuestionBankVO> getQuestionBankList(int page, int size, Integer status, String keyword) {
        Page<CustomQuestionBank> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CustomQuestionBank> wrapper = new LambdaQueryWrapper<>();
        Integer normalizedStatus = questionBankSupport.normalizeBankStatus(status);
        if (normalizedStatus != null) {
            wrapper.eq(CustomQuestionBank::getStatus, normalizedStatus);
        }
        if (StringUtils.hasText(keyword)) {
            String query = keyword.trim();
            wrapper.and(w -> w.like(CustomQuestionBank::getName, query)
                    .or()
                    .like(CustomQuestionBank::getOriginalFileName, query));
        }
        wrapper.orderByAsc(CustomQuestionBank::getStatus)
                .orderByDesc(CustomQuestionBank::getCreatedAt);

        Page<CustomQuestionBank> result = customQuestionBankMapper.selectPage(pageParam, wrapper);
        enrichOwners(result.getRecords());

        Page<AdminQuestionBankVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(bank -> AdminQuestionBankViewAssembler.toVO(bank, questionBankSupport))
                .toList());
        return voPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminOfficialQuestionImportVO importOfficialQuestionBank(
            MultipartFile file,
            String name,
            String category,
            Integer difficulty
    ) throws IOException {
        questionBankImportService.validateImportFile(file, MAX_FILE_SIZE);

        QuestionBankImportService.ImportPayload payload = questionBankImportService.analyzeFile(file.getOriginalFilename(), file.getBytes());
        if (payload.questions().isEmpty()) {
            throw new IllegalArgumentException("鏈В鏋愬埌鏈夋晥棰樼洰锛岃涓婁紶绗﹀悎妯℃澘鏍煎紡鐨?TXT 鏂囦欢");
        }

        String bankName = questionBankSupport.resolveBankName(name, file.getOriginalFilename());
        String normalizedCategory = questionBankSupport.normalizeCategory(category);
        String finalCategory = normalizedCategory == null ? "java" : normalizedCategory;
        Integer normalizedDifficulty = questionBankSupport.normalizeDifficulty(difficulty);
        int finalDifficulty = normalizedDifficulty == null ? 2 : normalizedDifficulty;

        LocalDateTime now = LocalDateTime.now();
        int inserted = 0;
        for (QuestionBankImportService.QuestionDraft draft : payload.questions()) {
            Question question = new Question();
            question.setTitle(draft.title());
            question.setContent(draft.content());
            question.setStandardAnswer(draft.standardAnswer());
            question.setDifficulty(finalDifficulty);
            question.setCategory(finalCategory);
            question.setTags(questionBankSupport.buildOfficialQuestionTags(bankName));
            question.setViewCount(0);
            question.setStatus(1);
            question.setSourceType(QuestionAccessSupport.SOURCE_OFFICIAL);
            question.setCustomBankId(null);
            question.setOwnerUserId(null);
            question.setCreatedAt(now);
            questionMapper.insert(question);
            inserted += 1;
        }

        AdminOfficialQuestionImportVO result = new AdminOfficialQuestionImportVO();
        result.setName(bankName);
        result.setCategory(finalCategory);
        result.setDifficulty(finalDifficulty);
        result.setParsedQuestionCount(payload.questions().size());
        result.setImportedQuestionCount(inserted);
        questionReadCacheSupport.evictAllQuestionReadCaches();
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminQuestionBankVO auditQuestionBank(Long id, AdminQuestionBankAuditRequest request) {
        CustomQuestionBank bank = customQuestionBankMapper.selectById(id);
        if (bank == null) {
            throw new NotFoundException("棰樺簱涓嶅瓨鍦?");
        }

        Integer targetStatus = questionBankSupport.normalizeBankStatus(request.getStatus());
        if (targetStatus == null || targetStatus == QuestionAccessSupport.BANK_STATUS_PENDING) {
            throw new IllegalArgumentException("瀹℃牳鐘舵€佷笉鍚堟硶");
        }

        String normalizedExistingCategory = questionBankSupport.normalizeCategory(bank.getCategory());
        if (normalizedExistingCategory != null) {
            bank.setCategory(normalizedExistingCategory);
        }

        if (StringUtils.hasText(request.getName())) {
            bank.setName(questionBankSupport.limitLength(request.getName().trim(), 120));
        }

        if (StringUtils.hasText(request.getCategory())) {
            String category = questionBankSupport.normalizeCategory(request.getCategory());
            if (category == null) {
                throw new IllegalArgumentException("棰樺簱鍒嗙被涓嶅悎娉?");
            }
            bank.setCategory(category);
        }

        if (request.getDifficulty() != null) {
            Integer difficulty = questionBankSupport.normalizeDifficulty(request.getDifficulty());
            if (difficulty == null) {
                throw new IllegalArgumentException("棰樺簱闅惧害涓嶅悎娉?");
            }
            bank.setDifficulty(difficulty);
        }

        String rejectReason = questionBankSupport.normalizeRejectReason(request.getRejectReason());
        if (targetStatus == QuestionAccessSupport.BANK_STATUS_REJECTED && !StringUtils.hasText(rejectReason)) {
            throw new IllegalArgumentException("椹冲洖鍘熷洜涓嶈兘涓虹┖");
        }
        if (targetStatus == QuestionAccessSupport.BANK_STATUS_REJECTED && bank.getImportedAt() != null) {
            throw new IllegalArgumentException("宸插鍏ョ殑棰樺簱涓嶈兘鍐嶆椹冲洖");
        }

        if (targetStatus == QuestionAccessSupport.BANK_STATUS_APPROVED && bank.getImportedAt() == null) {
            importQuestions(bank);
        }

        bank.setStatus(targetStatus);
        bank.setRejectReason(targetStatus == QuestionAccessSupport.BANK_STATUS_REJECTED ? rejectReason : null);
        bank.setAuditedAt(LocalDateTime.now());
        bank.setUpdatedAt(LocalDateTime.now());
        customQuestionBankMapper.updateById(bank);
        questionReadCacheSupport.evictAllQuestionReadCaches();
        questionReadCacheSupport.evictMyBanks(bank.getUserId());

        enrichOwners(List.of(bank));
        return AdminQuestionBankViewAssembler.toVO(bank, questionBankSupport);
    }

    private void importQuestions(CustomQuestionBank bank) {
        List<QuestionBankImportService.QuestionDraft> drafts = questionBankImportService.parseQuestions(bank.getRawContent());
        if (drafts.isEmpty()) {
            throw new IllegalArgumentException("鏈粠璇ラ搴撲腑瑙ｆ瀽鍒版湁鏁堥鐩?");
        }

        LocalDateTime now = LocalDateTime.now();
        String normalizedCategory = questionBankSupport.normalizeCategory(bank.getCategory());
        String finalCategory = normalizedCategory == null ? "java" : normalizedCategory;
        Integer normalizedDifficulty = questionBankSupport.normalizeDifficulty(bank.getDifficulty());
        int finalDifficulty = normalizedDifficulty == null ? 2 : normalizedDifficulty;

        int inserted = 0;
        for (QuestionBankImportService.QuestionDraft draft : drafts) {
            Question question = new Question();
            question.setTitle(draft.title());
            question.setContent(draft.content());
            question.setStandardAnswer(draft.standardAnswer());
            question.setDifficulty(finalDifficulty);
            question.setCategory(finalCategory);
            question.setTags(questionBankSupport.buildQuestionTags(bank.getName()));
            question.setViewCount(0);
            question.setStatus(1);
            question.setSourceType(QuestionAccessSupport.SOURCE_CUSTOM);
            question.setCustomBankId(bank.getId());
            question.setOwnerUserId(bank.getUserId());
            question.setCreatedAt(now);
            questionMapper.insert(question);
            inserted += 1;
        }

        bank.setImportedQuestionCount(inserted);
        bank.setImportedAt(now);
        bank.setQuestionCount(inserted);
        bank.setCategory(finalCategory);
        bank.setDifficulty(finalDifficulty);
    }

    private void enrichOwners(List<CustomQuestionBank> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream()
                .map(CustomQuestionBank::getUserId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left, HashMap::new));

        for (CustomQuestionBank record : records) {
            User owner = userMap.get(record.getUserId());
            if (owner != null) {
                record.setOwnerUsername(owner.getUsername());
                record.setOwnerNickname(owner.getNickname());
            }
        }
    }
}
