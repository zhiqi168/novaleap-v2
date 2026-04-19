package com.novaleap.api.module.admin.question.vo;

import lombok.Data;

@Data
public class AdminQuestionCategoryVO {
    private String code;
    private String name;
    private Boolean builtin;
    private Long questionCount;
    private Long bankCount;
    private Boolean deletable;
}
