package com.kapor.techtalk.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kapor.techtalk.model.RoleplaySession;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleplayStreamEvent {
    String type;
    String sessionId;
    String turnId;
    String userMessageId;
    String messageId;
    String delta;
    RoleplaySession.Message message;
    RoleplaySession.Evaluation evaluation;
    Boolean allObjectivesCompleted;
    String code;
    String messageText;
    Boolean retryable;
}
