package com.kapor.devvocab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultDto {
    private int score;
    private int correctAnswers;
    private int totalQuestions;
    private boolean passed;
    private LessonActivityProgressDto progress;
}
