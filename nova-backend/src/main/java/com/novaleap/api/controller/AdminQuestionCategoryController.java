package com.novaleap.api.controller;

import com.novaleap.api.common.Result;
import com.novaleap.api.module.admin.question.dto.AdminQuestionCategoryCreateRequest;
import com.novaleap.api.module.admin.question.service.AdminQuestionApplicationService;
import com.novaleap.api.module.admin.question.vo.AdminQuestionCategoryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/question-categories")
public class AdminQuestionCategoryController {

    private final AdminQuestionApplicationService adminQuestionApplicationService;

    public AdminQuestionCategoryController(AdminQuestionApplicationService adminQuestionApplicationService) {
        this.adminQuestionApplicationService = adminQuestionApplicationService;
    }

    @GetMapping
    public Result<List<AdminQuestionCategoryVO>> getQuestionCategoryList() {
        return Result.success(adminQuestionApplicationService.getQuestionCategoryList());
    }

    @PostMapping
    public Result<AdminQuestionCategoryVO> createQuestionCategory(
            @RequestBody @Valid AdminQuestionCategoryCreateRequest request
    ) {
        return Result.success(adminQuestionApplicationService.createQuestionCategory(request));
    }

    @DeleteMapping("/{code}")
    public Result<Void> deleteQuestionCategory(@PathVariable("code") String code) {
        adminQuestionApplicationService.deleteQuestionCategory(code);
        return Result.success();
    }
}
