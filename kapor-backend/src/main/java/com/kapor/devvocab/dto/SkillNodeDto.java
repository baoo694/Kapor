package com.kapor.devvocab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillNodeDto {
    private String id;
    private String title;
    private String titleVi;
    private String domain;
    private boolean isLocked;
    private List<String> prerequisites;
    private double completionPercent;
    private long totalLessons;
    private long completedLessons;
}
