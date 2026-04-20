package com.novaleap.api.module.admin.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novaleap.api.common.exception.NotFoundException;
import com.novaleap.api.entity.CustomQuestionBank;
import com.novaleap.api.entity.Question;
import com.novaleap.api.entity.QuestionCategory;
import com.novaleap.api.mapper.CustomQuestionBankMapper;
import com.novaleap.api.mapper.QuestionCategoryMapper;
import com.novaleap.api.mapper.QuestionMapper;
import com.novaleap.api.module.admin.question.assembler.AdminQuestionViewAssembler;
import com.novaleap.api.module.admin.question.dto.AdminQuestionCategoryCreateRequest;
import com.novaleap.api.module.admin.question.dto.AdminQuestionSaveRequest;
import com.novaleap.api.module.admin.question.support.AdminQuestionCategorySupport;
import com.novaleap.api.module.admin.question.vo.AdminQuestionCategoryVO;
import com.novaleap.api.module.admin.question.vo.AdminQuestionVO;
import com.novaleap.api.module.question.support.QuestionReadCacheSupport;
import com.novaleap.api.module.system.catalog.QuestionCategoryCatalog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminQuestionApplicationService {

    private static final int QUESTION_STATUS_DISABLED = 0;
    private static final int QUESTION_STATUS_ENABLED = 1;
    private static final String DEFAULT_CATEGORY = "java";

    private final QuestionMapper questionMapper;
    private final QuestionCategoryMapper questionCategoryMapper;
    private final CustomQuestionBankMapper customQuestionBankMapper;
    private final QuestionReadCacheSupport questionReadCacheSupport;

    public AdminQuestionApplicationService(
            QuestionMapper questionMapper,
            QuestionCategoryMapper questionCategoryMapper,
            CustomQuestionBankMapper customQuestionBankMapper,
            QuestionReadCacheSupport questionReadCacheSupport
    ) {
        this.questionMapper = questionMapper;
        this.questionCategoryMapper = questionCategoryMapper;
        this.customQuestionBankMapper = customQuestionBankMapper;
        this.questionReadCacheSupport = questionReadCacheSupport;
    }

    public Page<AdminQuestionVO> getQuestionList(Integer page, Integer size, String category, Integer difficulty, String keyword) {
        Page<Question> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();

        List<String> categoryFilterCodes = AdminQuestionCategorySupport.resolveFilterCodes(category);
        if (!categoryFilterCodes.isEmpty()) {
            if (categoryFilterCodes.size() == 1) {
                wrapper.eq(Question::getCategory, categoryFilterCodes.get(0));
            } else {
                wrapper.in(Question::getCategory, categoryFilterCodes);
            }
        }
        if (difficulty != null) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        if (!isBlank(keyword)) {
            wrapper.like(Question::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(Question::getCreatedAt);

        Page<Question> result = questionMapper.selectPage(pageParam, wrapper);
        return toQuestionPage(result);
    }

    public AdminQuestionVO getQuestionDetail(Long id) {
        return toVO(loadQuestion(id), loadCategoryOptions());
    }

    public AdminQuestionVO createQuestion(AdminQuestionSaveRequest request) {
        String category = AdminQuestionCategorySupport.normalizeCategory(request.getCategory(), questionCategoryMapper);
        Integer difficulty = normalizeDifficulty(request.getDifficulty());
        Integer status = normalizeQuestionStatus(request.getStatus());

        validateQuestionRequest(request, category, difficulty, status);

        Question question = new Question();
        applyQuestionFields(question, request, category, difficulty, status);
        question.setCreatedAt(LocalDateTime.now());
        questionMapper.insert(question);
        questionReadCacheSupport.evictAllQuestionReadCaches();
        questionReadCacheSupport.resetOfficialQuestionViewCount(question.getId(), question.getViewCount());
        return toVO(question, loadCategoryOptions());
    }

    public AdminQuestionVO updateQuestion(Long id, AdminQuestionSaveRequest request) {
        Question question = loadQuestion(id);

        String category = AdminQuestionCategorySupport.normalizeCategory(request.getCategory(), questionCategoryMapper);
        Integer difficulty = normalizeDifficulty(request.getDifficulty());
        Integer status = normalizeQuestionStatus(request.getStatus());

        validateQuestionRequest(request, category, difficulty, status);
        applyQuestionFields(question, request, category, difficulty, status);
        questionMapper.updateById(question);
        questionReadCacheSupport.evictAllQuestionReadCaches();
        questionReadCacheSupport.evictQuestionDetail(id);
        questionReadCacheSupport.evictQuestionAnswer(id);
        questionReadCacheSupport.resetOfficialQuestionViewCount(id, question.getViewCount());
        return toVO(question, loadCategoryOptions());
    }

    public void deleteQuestion(Long id) {
        loadQuestion(id);
        questionMapper.deleteById(id);
        questionReadCacheSupport.evictAllQuestionReadCaches();
        questionReadCacheSupport.evictQuestionDetail(id);
        questionReadCacheSupport.evictQuestionAnswer(id);
        questionReadCacheSupport.evictOfficialQuestionViewCount(id);
    }

    public List<AdminQuestionCategoryVO> getQuestionCategoryList() {
        List<AdminQuestionCategoryVO> options = AdminQuestionCategorySupport.loadOptions(questionCategoryMapper);
        Map<String, Long> questionCountMap = loadQuestionCountByCategory();
        Map<String, Long> bankCountMap = loadBankCountByCategory();
        for (AdminQuestionCategoryVO option : options) {
            String code = AdminQuestionCategorySupport.canonicalizeCategoryCode(option.getCode());
            long questionCount = questionCountMap.getOrDefault(code, 0L);
            long bankCount = bankCountMap.getOrDefault(code, 0L);
            boolean builtin = QuestionCategoryCatalog.isBuiltin(code);
            option.setBuiltin(builtin);
            option.setQuestionCount(questionCount);
            option.setBankCount(bankCount);
            option.setDeletable(!builtin && questionCount == 0L && bankCount == 0L);
        }
        return options;
    }

    public AdminQuestionCategoryVO createQuestionCategory(AdminQuestionCategoryCreateRequest request) {
        String name = trim(request.getName());
        String rawCode = trim(request.getCode());
        String code = AdminQuestionCategorySupport.canonicalizeCategoryCode(
                AdminQuestionCategorySupport.normalizeCategoryCode(isBlank(rawCode) ? name : rawCode)
        );

        if (isBlank(code)) {
            throw new IllegalArgumentException("分类编码不合法");
        }
        if (code.length() > 64) {
            throw new IllegalArgumentException("分类编码长度不能超过 64 个字符");
        }

        LambdaQueryWrapper<QuestionCategory> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(QuestionCategory::getCode, code);
        if (questionCategoryMapper.selectCount(codeWrapper) > 0) {
            throw new IllegalArgumentException("分类编码已存在");
        }

        LambdaQueryWrapper<QuestionCategory> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(QuestionCategory::getName, name);
        if (questionCategoryMapper.selectCount(nameWrapper) > 0) {
            throw new IllegalArgumentException("分类名称已存在");
        }

        QuestionCategory category = new QuestionCategory();
        category.setCode(code);
        category.setName(name);
        category.setSortOrder(AdminQuestionCategorySupport.loadOptions(questionCategoryMapper).size() + 1);
        category.setEnabled(1);
        LocalDateTime now = LocalDateTime.now();
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        questionCategoryMapper.insert(category);

        AdminQuestionCategoryVO vo = new AdminQuestionCategoryVO();
        vo.setCode(category.getCode());
        vo.setName(category.getName());
        vo.setBuiltin(false);
        vo.setQuestionCount(0L);
        vo.setBankCount(0L);
        vo.setDeletable(true);
        questionReadCacheSupport.evictQuestionCategories();
        return vo;
    }

    public void deleteQuestionCategory(String rawCode) {
        String code = AdminQuestionCategorySupport.canonicalizeCategoryCode(rawCode);
        if (isBlank(code)) {
            throw new IllegalArgumentException("分类编码不能为空");
        }
        if (QuestionCategoryCatalog.isBuiltin(code)) {
            throw new IllegalArgumentException("内置分类不允许删除");
        }

        LambdaQueryWrapper<QuestionCategory> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(QuestionCategory::getCode, code);
        QuestionCategory category = questionCategoryMapper.selectOne(categoryWrapper);
        if (category == null) {
            throw new NotFoundException("分类不存在");
        }

        long questionCount = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .eq(Question::getCategory, code));
        if (questionCount > 0) {
            throw new IllegalArgumentException("该分类下仍有题目，不能删除");
        }

        long bankCount = customQuestionBankMapper.selectCount(new LambdaQueryWrapper<CustomQuestionBank>()
                .eq(CustomQuestionBank::getCategory, code));
        if (bankCount > 0) {
            throw new IllegalArgumentException("该分类下仍有关联题库，不能删除");
        }

        questionCategoryMapper.deleteById(category.getId());
        questionReadCacheSupport.evictQuestionCategories();
    }

    private Question loadQuestion(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new NotFoundException("题目不存在");
        }
        return question;
    }

    private void validateQuestionRequest(AdminQuestionSaveRequest request, String category, Integer difficulty, Integer status) {
        if (!isBlank(request.getCategory()) && category == null) {
            throw new IllegalArgumentException("题目分类不合法");
        }
        if (request.getDifficulty() != null && difficulty == null) {
            throw new IllegalArgumentException("题目难度不合法");
        }
        if (request.getStatus() != null && status == null) {
            throw new IllegalArgumentException("题目状态不合法");
        }
    }

    private void applyQuestionFields(Question question, AdminQuestionSaveRequest request, String category, Integer difficulty, Integer status) {
        question.setTitle(trim(request.getTitle()));
        question.setContent(request.getContent());
        question.setStandardAnswer(trim(request.getStandardAnswer()));
        question.setCategory(category == null ? DEFAULT_CATEGORY : category);
        question.setDifficulty(difficulty == null ? 2 : difficulty);
        question.setTags(request.getTags());
        question.setViewCount(nonNegativeInt(request.getViewCount()));
        question.setStatus(status == null ? QUESTION_STATUS_ENABLED : status);
    }

    private Page<AdminQuestionVO> toQuestionPage(Page<Question> sourcePage) {
        Page<AdminQuestionVO> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        List<AdminQuestionCategoryVO> categoryOptions = loadCategoryOptions();
        List<AdminQuestionVO> records = sourcePage.getRecords() == null
                ? Collections.emptyList()
                : sourcePage.getRecords().stream().map(item -> toVO(item, categoryOptions)).toList();
        targetPage.setRecords(records);
        return targetPage;
    }

    private AdminQuestionVO toVO(Question question, List<AdminQuestionCategoryVO> categoryOptions) {
        String category = AdminQuestionCategorySupport.canonicalizeCategoryCode(question.getCategory());
        question.setCategory(category);
        String categoryName = AdminQuestionCategorySupport.resolveCategoryName(category, categoryOptions);
        return AdminQuestionViewAssembler.toVO(question, categoryName);
    }

    private List<AdminQuestionCategoryVO> loadCategoryOptions() {
        return AdminQuestionCategorySupport.loadOptions(questionCategoryMapper);
    }

    private Map<String, Long> loadQuestionCountByCategory() {
        Map<String, Long> result = new HashMap<>();
        QueryWrapper<Question> wrapper = new QueryWrapper<>();
        wrapper.select("category AS category", "COUNT(*) AS total")
                .isNotNull("category")
                .groupBy("category");
        List<Map<String, Object>> rows = questionMapper.selectMaps(wrapper);
        for (Map<String, Object> row : rows) {
            String code = AdminQuestionCategorySupport.canonicalizeCategoryCode(asString(row.get("category")));
            if (!isBlank(code)) {
                result.put(code, asLong(row.get("total")));
            }
        }
        return result;
    }

    private Map<String, Long> loadBankCountByCategory() {
        Map<String, Long> result = new HashMap<>();
        QueryWrapper<CustomQuestionBank> wrapper = new QueryWrapper<>();
        wrapper.select("category AS category", "COUNT(*) AS total")
                .isNotNull("category")
                .groupBy("category");
        List<Map<String, Object>> rows = customQuestionBankMapper.selectMaps(wrapper);
        for (Map<String, Object> row : rows) {
            String code = AdminQuestionCategorySupport.canonicalizeCategoryCode(asString(row.get("category")));
            if (!isBlank(code)) {
                result.put(code, asLong(row.get("total")));
            }
        }
        return result;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private Integer normalizeDifficulty(Integer difficulty) {
        if (difficulty == null) {
            return null;
        }
        return difficulty >= 1 && difficulty <= 3 ? difficulty : null;
    }

    private Integer normalizeQuestionStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return status == QUESTION_STATUS_DISABLED || status == QUESTION_STATUS_ENABLED ? status : null;
    }

    private int nonNegativeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
