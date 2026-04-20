package com.novaleap.api.module.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novaleap.api.common.exception.NotFoundException;
import com.novaleap.api.entity.CustomQuestionBank;
import com.novaleap.api.entity.Question;
import com.novaleap.api.entity.QuestionCategory;
import com.novaleap.api.mapper.CustomQuestionBankMapper;
import com.novaleap.api.mapper.QuestionCategoryMapper;
import com.novaleap.api.mapper.QuestionMapper;
import com.novaleap.api.module.question.assembler.QuestionViewAssembler;
import com.novaleap.api.module.question.support.QuestionReadCacheSupport;
import com.novaleap.api.module.question.vo.QuestionAnswerVO;
import com.novaleap.api.module.question.vo.QuestionCategoryOptionVO;
import com.novaleap.api.module.question.vo.QuestionDetailVO;
import com.novaleap.api.module.question.vo.QuestionListItemVO;
import com.novaleap.api.module.question.vo.QuestionViewCountVO;
import com.novaleap.api.module.system.catalog.QuestionCategoryCatalog;
import com.novaleap.api.service.QuestionAccessSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class QuestionApplicationService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int HOT_KEY_RETRY_TIMES = 3;
    private static final long HOT_KEY_RETRY_SLEEP_MS = 40L;
    private static final String QUESTION_NOT_FOUND_MESSAGE = "\u9898\u76ee\u4e0d\u5b58\u5728";
    private static final String NO_RANDOM_QUESTION_MESSAGE = "\u5f53\u524d\u7b5b\u9009\u6761\u4ef6\u4e0b\u6682\u65e0\u9898\u76ee";
    private static final Logger log = LoggerFactory.getLogger(QuestionApplicationService.class);

    private final QuestionMapper questionMapper;
    private final CustomQuestionBankMapper customQuestionBankMapper;
    private final QuestionCategoryMapper questionCategoryMapper;
    private final QuestionAccessSupport questionAccessSupport;
    private final QuestionReadCacheSupport questionReadCacheSupport;

    public QuestionApplicationService(
            QuestionMapper questionMapper,
            CustomQuestionBankMapper customQuestionBankMapper,
            QuestionCategoryMapper questionCategoryMapper,
            QuestionAccessSupport questionAccessSupport,
            QuestionReadCacheSupport questionReadCacheSupport
    ) {
        this.questionMapper = questionMapper;
        this.customQuestionBankMapper = customQuestionBankMapper;
        this.questionCategoryMapper = questionCategoryMapper;
        this.questionAccessSupport = questionAccessSupport;
        this.questionReadCacheSupport = questionReadCacheSupport;
    }

    public Page<QuestionListItemVO> getQuestionList(
            Integer page,
            Integer size,
            String category,
            Integer difficulty,
            String keyword,
            Long bankId,
            Authentication authentication
    ) {
        long startNs = System.nanoTime();
        CustomQuestionBank bank = null;
        String cacheKey;
        if (bankId != null) {
            bank = questionAccessSupport.resolveAccessibleBank(bankId, authentication);
            if (bank == null) {
                log.info("[perf][question-list] bank-inaccessible bankId={} page={} size={} category={} difficulty={} keyword={} tookMs={}",
                        bankId, normalizePage(page), normalizeSize(size), safe(category), difficulty, safe(keyword),
                        elapsedMs(startNs));
                return emptyQuestionPage(page, size);
            }
            cacheKey = questionReadCacheSupport.bankListKey(bankId, page, size, category, difficulty, keyword);
        } else {
            cacheKey = questionReadCacheSupport.officialListKey(page, size, category, difficulty, keyword);
        }

        Page<QuestionListItemVO> cached = questionReadCacheSupport.readQuestionListPage(cacheKey);
        if (cached != null) {
            log.info("[perf][question-list] cache-hit bankId={} page={} size={} category={} difficulty={} keyword={} records={} total={} tookMs={}",
                    bankId, normalizePage(page), normalizeSize(size), safe(category), difficulty, safe(keyword),
                    cached.getRecords() == null ? 0 : cached.getRecords().size(), cached.getTotal(), elapsedMs(startNs));
            return cached;
        }

        Page<Question> pageParam = new Page<>(normalizePage(page), normalizeSize(size));
        LambdaQueryWrapper<Question> wrapper = buildQuestionFilterWrapper(category, difficulty, keyword, bankId, bank != null);
        wrapper.orderByDesc(Question::getCreatedAt);

        Page<Question> result = questionMapper.selectPage(pageParam, wrapper);
        Page<QuestionListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::toQuestionListItemVO)
                .toList());
        questionReadCacheSupport.writeQuestionListPage(cacheKey, voPage);
        log.info("[perf][question-list] cache-miss bankId={} page={} size={} category={} difficulty={} keyword={} records={} total={} tookMs={}",
                bankId, normalizePage(page), normalizeSize(size), safe(category), difficulty, safe(keyword),
                voPage.getRecords() == null ? 0 : voPage.getRecords().size(), voPage.getTotal(), elapsedMs(startNs));
        return voPage;
    }

    public List<QuestionCategoryOptionVO> getQuestionCategoryList() {
        long startNs = System.nanoTime();
        List<QuestionCategoryOptionVO> cached = questionReadCacheSupport.readQuestionCategories();
        if (cached != null) {
            log.info("[perf][question-categories] cache-hit size={} tookMs={}", cached.size(), elapsedMs(startNs));
            return cached;
        }

        List<QuestionCategoryOptionVO> options = loadQuestionCategoryOptions();
        questionReadCacheSupport.writeQuestionCategories(options);
        log.info("[perf][question-categories] cache-miss size={} tookMs={}", options.size(), elapsedMs(startNs));
        return options;
    }

    public QuestionDetailVO getQuestionDetail(Long id, Authentication authentication) {
        long startNs = System.nanoTime();
        QuestionDetailVO cached = questionReadCacheSupport.readOfficialQuestionDetail(id);
        if (cached != null) {
            log.info("[perf][question-detail] cache-hit id={} tookMs={}", id, elapsedMs(startNs));
            return cached;
        }

        String lockToken = questionReadCacheSupport.tryLockOfficialQuestionDetail(id);
        if (lockToken == null) {
            QuestionDetailVO waitedCache = waitForOfficialQuestionDetailCache(id);
            if (waitedCache != null) {
                log.info("[perf][question-detail] cache-hit-after-wait id={} tookMs={}", id, elapsedMs(startNs));
                return waitedCache;
            }
        }

        try {
            if (lockToken != null) {
                QuestionDetailVO doubleChecked = questionReadCacheSupport.readOfficialQuestionDetail(id);
                if (doubleChecked != null) {
                    log.info("[perf][question-detail] cache-hit-after-lock id={} tookMs={}", id, elapsedMs(startNs));
                    return doubleChecked;
                }
            }

            Question question = questionAccessSupport.resolveAccessibleQuestion(id, authentication);
            if (question == null) {
                throw new NotFoundException(QUESTION_NOT_FOUND_MESSAGE);
            }

            QuestionDetailVO detail = toQuestionDetailVO(question);
            if (isOfficialQuestion(question)) {
                questionReadCacheSupport.writeOfficialQuestionDetail(id, detail);
                questionReadCacheSupport.writeOfficialQuestionAnswer(id, buildAnswerVO(question));
            }
            log.info("[perf][question-detail] cache-miss id={} sourceType={} tookMs={}",
                    id, safe(question.getSourceType()), elapsedMs(startNs));
            return detail;
        } finally {
            questionReadCacheSupport.unlockOfficialQuestionDetail(id, lockToken);
        }
    }

    public QuestionViewCountVO increaseQuestionView(Long id, Authentication authentication) {
        long startNs = System.nanoTime();
        Integer cachedViewCount = questionReadCacheSupport.readOfficialQuestionViewCount(id);
        if (cachedViewCount != null) {
            int nextCount = cachedViewCount + 1;
            questionReadCacheSupport.incrementPendingViewCount(id);
            questionReadCacheSupport.touchOfficialQuestionDetailViewCount(id, nextCount);
            log.info("[perf][question-view] buffered id={} nextCount={} tookMs={}", id, nextCount, elapsedMs(startNs));
            return new QuestionViewCountVO(id, nextCount);
        }

        Question question = questionAccessSupport.resolveAccessibleQuestion(id, authentication);
        if (question == null) {
            throw new NotFoundException(QUESTION_NOT_FOUND_MESSAGE);
        }

        questionReadCacheSupport.incrementPendingViewCount(id);
        int nextCount = (question.getViewCount() == null ? 0 : question.getViewCount()) + 1;
        if (isOfficialQuestion(question)) {
            QuestionDetailVO detail = toQuestionDetailVO(question);
            detail.setViewCount(nextCount);
            questionReadCacheSupport.writeOfficialQuestionDetail(id, detail);
        }
        log.info("[perf][question-view] fallback-buffered id={} nextCount={} sourceType={} tookMs={}",
                id, nextCount, safe(question.getSourceType()), elapsedMs(startNs));
        return new QuestionViewCountVO(id, nextCount);
    }

    public QuestionDetailVO drawRandomQuestion(
            String category,
            Integer difficulty,
            Long bankId,
            Authentication authentication
    ) {
        long startNs = System.nanoTime();
        CustomQuestionBank bank = null;
        String cacheKey;
        if (bankId != null) {
            bank = questionAccessSupport.resolveAccessibleBank(bankId, authentication);
            if (bank == null) {
                throw new NotFoundException(NO_RANDOM_QUESTION_MESSAGE);
            }
            cacheKey = questionReadCacheSupport.bankRandomPoolKey(bankId, category, difficulty);
        } else {
            cacheKey = questionReadCacheSupport.officialRandomPoolKey(category, difficulty);
        }

        List<Long> candidateIds = questionReadCacheSupport.readRandomPool(cacheKey);
        boolean poolCacheHit = candidateIds != null && !candidateIds.isEmpty();
        if (candidateIds == null || candidateIds.isEmpty()) {
            LambdaQueryWrapper<Question> wrapper = buildQuestionFilterWrapper(category, difficulty, null, bankId, bank != null);
            wrapper.select(Question::getId).orderByAsc(Question::getId);
            candidateIds = questionMapper.selectList(wrapper).stream()
                    .map(Question::getId)
                    .filter(id -> id != null && id > 0)
                    .toList();
            if (!candidateIds.isEmpty()) {
                questionReadCacheSupport.writeRandomPool(cacheKey, candidateIds);
            }
        }

        if (candidateIds == null || candidateIds.isEmpty()) {
            throw new NotFoundException(NO_RANDOM_QUESTION_MESSAGE);
        }

        Long randomId = candidateIds.get(candidateIds.size() <= 1
                ? 0
                : ThreadLocalRandom.current().nextInt(candidateIds.size()));

        if (bankId == null) {
            QuestionDetailVO cached = questionReadCacheSupport.readOfficialQuestionDetail(randomId);
            if (cached != null) {
                log.info("[perf][question-random] poolCacheHit={} detailCacheHit=true bankId={} category={} difficulty={} candidateSize={} questionId={} tookMs={}",
                        poolCacheHit, bankId, safe(category), difficulty, candidateIds.size(), randomId, elapsedMs(startNs));
                return cached;
            }
        }

        Question question = questionMapper.selectById(randomId);
        if (question == null || question.getStatus() == null || question.getStatus() != 1) {
            throw new NotFoundException(NO_RANDOM_QUESTION_MESSAGE);
        }

        QuestionDetailVO detail = toQuestionDetailVO(question);
        if (isOfficialQuestion(question)) {
            questionReadCacheSupport.writeOfficialQuestionDetail(question.getId(), detail);
            questionReadCacheSupport.writeOfficialQuestionAnswer(question.getId(), buildAnswerVO(question));
        }
        log.info("[perf][question-random] poolCacheHit={} detailCacheHit=false bankId={} category={} difficulty={} candidateSize={} questionId={} tookMs={}",
                poolCacheHit, bankId, safe(category), difficulty, candidateIds.size(), question.getId(), elapsedMs(startNs));
        return detail;
    }

    public QuestionAnswerVO getQuestionAnswer(Long id, Authentication authentication) {
        long startNs = System.nanoTime();
        QuestionAnswerVO cached = questionReadCacheSupport.readOfficialQuestionAnswer(id);
        if (cached != null) {
            log.info("[perf][question-answer] cache-hit id={} tookMs={}", id, elapsedMs(startNs));
            return cached;
        }

        String lockToken = questionReadCacheSupport.tryLockOfficialQuestionAnswer(id);
        if (lockToken == null) {
            QuestionAnswerVO waitedCache = waitForOfficialQuestionAnswerCache(id);
            if (waitedCache != null) {
                log.info("[perf][question-answer] cache-hit-after-wait id={} tookMs={}", id, elapsedMs(startNs));
                return waitedCache;
            }
        }

        try {
            if (lockToken != null) {
                QuestionAnswerVO doubleChecked = questionReadCacheSupport.readOfficialQuestionAnswer(id);
                if (doubleChecked != null) {
                    log.info("[perf][question-answer] cache-hit-after-lock id={} tookMs={}", id, elapsedMs(startNs));
                    return doubleChecked;
                }
            }

            Question question = questionAccessSupport.resolveAccessibleQuestion(id, authentication);
            if (question == null) {
                throw new NotFoundException(QUESTION_NOT_FOUND_MESSAGE);
            }

            QuestionAnswerVO answerVO = buildAnswerVO(question);
            if (isOfficialQuestion(question)) {
                questionReadCacheSupport.writeOfficialQuestionAnswer(id, answerVO);
            }
            log.info("[perf][question-answer] cache-miss id={} sourceType={} tookMs={}",
                    id, safe(question.getSourceType()), elapsedMs(startNs));
            return answerVO;
        } finally {
            questionReadCacheSupport.unlockOfficialQuestionAnswer(id, lockToken);
        }
    }

    private String resolveSourceLabel(Question question) {
        if (QuestionAccessSupport.SOURCE_CUSTOM.equalsIgnoreCase(safe(question.getSourceType()))) {
            CustomQuestionBank bank = question.getCustomBankId() == null
                    ? null
                    : customQuestionBankMapper.selectById(question.getCustomBankId());
            if (bank != null && bank.getName() != null && !bank.getName().isBlank()) {
                return "\u81ea\u5b9a\u4e49\u9898\u5e93 / " + bank.getName();
            }
            return "\u81ea\u5b9a\u4e49\u9898\u5e93";
        }
        return "\u5b98\u65b9\u9898\u5e93";
    }

    private QuestionAnswerVO buildAnswerVO(Question question) {
        String answer = question.getStandardAnswer();
        if (answer == null || answer.isBlank()) {
            answer = question.getContent();
        }
        answer = sanitizeOfficialAnswer(answer);
        return QuestionViewAssembler.toAnswerVO(
                question,
                answer == null ? "" : answer,
                resolveSourceLabel(question)
        );
    }

    private String sanitizeOfficialAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer
                .replaceFirst("^\\s*\\[(\\u6570\\u636e\\u5e93\\u6807\\u51c6\\u7b54\\u6848|\\u6807\\u51c6\\u7b54\\u6848)]\\s*", "")
                .replaceFirst("^\\s*(\\u6570\\u636e\\u5e93\\u6807\\u51c6\\u7b54\\u6848|\\u6807\\u51c6\\u7b54\\u6848|answer)\\s*[:\uFF1A]?\\s*", "")
                .trim();
    }

    private List<QuestionCategoryOptionVO> loadQuestionCategoryOptions() {
        List<QuestionCategory> dbRows;
        try {
            LambdaQueryWrapper<QuestionCategory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(QuestionCategory::getEnabled, 1)
                    .orderByAsc(QuestionCategory::getSortOrder)
                    .orderByAsc(QuestionCategory::getId);
            dbRows = questionCategoryMapper.selectList(wrapper);
        } catch (Exception ignore) {
            dbRows = Collections.emptyList();
        }

        if (dbRows != null && !dbRows.isEmpty()) {
            Map<String, String> deduped = new LinkedHashMap<>();
            for (QuestionCategory row : dbRows) {
                String code = QuestionCategoryCatalog.canonicalize(row.getCode());
                String name = sanitizeCategoryName(code, row.getName());
                if (code.isBlank() || name.isBlank()) {
                    continue;
                }
                deduped.putIfAbsent(code, name);
            }
            if (!deduped.isEmpty()) {
                List<QuestionCategoryOptionVO> options = new ArrayList<>();
                for (Map.Entry<String, String> entry : deduped.entrySet()) {
                    options.add(new QuestionCategoryOptionVO(entry.getKey(), entry.getValue()));
                }
                return options;
            }
        }

        List<QuestionCategoryOptionVO> fallback = new ArrayList<>();
        for (String code : QuestionCategoryCatalog.builtinCodes()) {
            fallback.add(new QuestionCategoryOptionVO(code, QuestionCategoryCatalog.builtinLabel(code)));
        }
        return fallback;
    }

    private String sanitizeCategoryName(String code, String rawName) {
        String key = QuestionCategoryCatalog.canonicalize(code);
        if (key.isBlank()) {
            return "";
        }
        if (QuestionCategoryCatalog.isBuiltin(key)) {
            return QuestionCategoryCatalog.builtinLabel(key);
        }

        String name = safe(rawName);
        if (name.isBlank()) {
            return key;
        }

        boolean onlyQuestionMarks = name.chars().allMatch(ch -> ch == '?');
        if (name.contains("\uFFFD") || onlyQuestionMarks) {
            return key;
        }
        return name;
    }

    private QuestionListItemVO toQuestionListItemVO(Question question) {
        return QuestionViewAssembler.toListItemVO(
                question,
                QuestionCategoryCatalog.canonicalize(question.getCategory())
        );
    }

    private QuestionDetailVO toQuestionDetailVO(Question question) {
        return QuestionViewAssembler.toDetailVO(
                question,
                QuestionCategoryCatalog.canonicalize(question.getCategory())
        );
    }

    private LambdaQueryWrapper<Question> buildQuestionFilterWrapper(
            String category,
            Integer difficulty,
            String keyword,
            Long bankId,
            boolean customBankResolved
    ) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getStatus, 1);

        if (bankId != null && customBankResolved) {
            wrapper.eq(Question::getSourceType, QuestionAccessSupport.SOURCE_CUSTOM);
            wrapper.eq(Question::getCustomBankId, bankId);
        } else {
            wrapper.and(w -> w.eq(Question::getSourceType, QuestionAccessSupport.SOURCE_OFFICIAL)
                    .or()
                    .isNull(Question::getSourceType));
        }

        List<String> categoryFilterCodes = QuestionCategoryCatalog.expandFilterCodes(category);
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
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Question::getTitle, keyword.trim());
        }
        return wrapper;
    }

    private Page<QuestionListItemVO> emptyQuestionPage(Integer page, Integer size) {
        Page<QuestionListItemVO> emptyPage = new Page<>(normalizePage(page), normalizeSize(size), 0);
        emptyPage.setRecords(Collections.emptyList());
        return emptyPage;
    }

    private boolean isOfficialQuestion(Question question) {
        return question != null && !QuestionAccessSupport.SOURCE_CUSTOM.equalsIgnoreCase(safe(question.getSourceType()));
    }

    private QuestionDetailVO waitForOfficialQuestionDetailCache(Long id) {
        for (int i = 0; i < HOT_KEY_RETRY_TIMES; i++) {
            sleepQuietly(HOT_KEY_RETRY_SLEEP_MS);
            QuestionDetailVO cached = questionReadCacheSupport.readOfficialQuestionDetail(id);
            if (cached != null) {
                return cached;
            }
        }
        return null;
    }

    private QuestionAnswerVO waitForOfficialQuestionAnswerCache(Long id) {
        for (int i = 0; i < HOT_KEY_RETRY_TIMES; i++) {
            sleepQuietly(HOT_KEY_RETRY_SLEEP_MS);
            QuestionAnswerVO cached = questionReadCacheSupport.readOfficialQuestionAnswer(id);
            if (cached != null) {
                return cached;
            }
        }
        return null;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private int normalizePage(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private int normalizeSize(Integer value) {
        if (value == null || value < 1) {
            return 10;
        }
        return Math.min(value, MAX_PAGE_SIZE);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }
}
