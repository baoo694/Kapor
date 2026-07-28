package com.kapor.devvocab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonActivityProgressDto {
    private String lessonId;
    private boolean studyCompleted;
    private boolean quizPassed;
    private boolean lessonCompleted;
    private int bestQuizScore;
    private int quizAttempts;
    private int bestMatchAccuracy;
    private int matchingAttempts;
}
