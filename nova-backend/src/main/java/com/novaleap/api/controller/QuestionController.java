package com.novaleap.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novaleap.api.common.Result;
import com.novaleap.api.module.question.service.QuestionApplicationService;
import com.novaleap.api.module.question.vo.QuestionAnswerVO;
import com.novaleap.api.module.question.vo.QuestionCategoryOptionVO;
import com.novaleap.api.module.question.vo.QuestionDetailVO;
import com.novaleap.api.module.question.vo.QuestionListItemVO;
import com.novaleap.api.module.question.vo.QuestionViewCountVO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionApplicationService questionApplicationService;

    public QuestionController(QuestionApplicationService questionApplicationService) {
        this.questionApplicationService = questionApplicationService;
    }

    @GetMapping
    public Result<Page<QuestionListItemVO>> getQuestionList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "difficulty", required = false) Integer difficulty,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "bankId", required = false) Long bankId,
            Authentication authentication
    ) {
        return Result.success(questionApplicationService.getQuestionList(
                page, size, category, difficulty, keyword, bankId, authentication
        ));
    }

    @GetMapping("/categories")
    public Result<List<QuestionCategoryOptionVO>> getQuestionCategoryList() {
        return Result.success(questionApplicationService.getQuestionCategoryList());
    }

    @GetMapping("/{id:\\d+}")
    public Result<QuestionDetailVO> getQuestionDetail(@PathVariable(value = "id") Long id, Authentication authentication) {
        return Result.success(questionApplicationService.getQuestionDetail(id, authentication));
    }

    @PostMapping("/{id:\\d+}/view")
    public Result<QuestionViewCountVO> increaseQuestionView(@PathVariable(value = "id") Long id, Authentication authentication) {
        return Result.success(questionApplicationService.increaseQuestionView(id, authentication));
    }

    @GetMapping("/random")
    public Result<QuestionDetailVO> drawRandomQuestion(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "difficulty", required = false) Integer difficulty,
            @RequestParam(value = "bankId", required = false) Long bankId,
            Authentication authentication
    ) {
        return Result.success(questionApplicationService.drawRandomQuestion(category, difficulty, bankId, authentication));
    }

    @GetMapping("/{id:\\d+}/answer")
    public Result<QuestionAnswerVO> getQuestionAnswer(@PathVariable(value = "id") Long id, Authentication authentication) {
        return Result.success(questionApplicationService.getQuestionAnswer(id, authentication));
    }

    @GetMapping("/hot")
    public Result<List<QuestionListItemVO>> getHotQuestionList(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit,
            Authentication authentication
    ) {
        return Result.success(questionApplicationService.getHotQuestionList(limit, authentication));
    }
}
