package com.kapor.techtalk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartRoleplayRequest {
    @NotBlank(message = "Scenario id is required")
    private String scenarioId;
    private boolean testMode;
}
