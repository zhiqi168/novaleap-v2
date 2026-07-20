package com.novaleap.api.module.ai.vo;

import lombok.Data;
import java.util.List;

@Data
public class AiCoachHistoryItemVO {

    private String role;
    private String content;
    private String mode;
    private String topic;
    private String sessionId;
    private String timestamp;
    private List<Object> questions;
    private List<Object> notes;
}
