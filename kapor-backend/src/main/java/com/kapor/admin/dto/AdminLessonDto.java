package com.kapor.admin.dto;

import com.kapor.devvocab.model.Lesson;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLessonDto {

    private String id;

    @NotBlank(message = "Topic is required")
    private String topicId;

    @NotBlank(message = "Korean title is required")
    private String title;

    @NotBlank(message = "Vietnamese title is required")
    private String titleVi;

    private String content;
    private String contentVi;

    @Min(value = 0, message = "Order must be zero or greater")
    private int order;

    @Builder.Default
    private List<Lesson.VocabularyItem> vocabulary = new ArrayList<>();

    @Builder.Default
    private List<Lesson.Exercise> exercises = new ArrayList<>();

    private String topicTitle;
    private String topicTitleVi;
    private String domain;
}
