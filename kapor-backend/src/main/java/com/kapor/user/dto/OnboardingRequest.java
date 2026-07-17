package com.kapor.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingRequest {
    private List<String> learningGoals;
    private Integer dailyGoalMinutes;
}
