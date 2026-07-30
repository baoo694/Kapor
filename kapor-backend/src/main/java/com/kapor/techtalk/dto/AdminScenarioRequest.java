package com.kapor.techtalk.dto;

import com.kapor.techtalk.model.TechTalkScenario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminScenarioRequest {
    @NotBlank private String title;
    @NotBlank private String titleVi;
    @NotBlank private String domain;
    @NotBlank private String difficulty;
    @NotNull private Integer order = 0;
    @Valid @NotNull private TechTalkScenario.Persona persona = new TechTalkScenario.Persona();
    private String missionVi;
    @Valid private TechTalkScenario.Mission mission = new TechTalkScenario.Mission();
    @Size(max = 20) private List<String> objectives = new ArrayList<>();
    @Size(max = 50) private List<String> requiredVocabulary = new ArrayList<>();
    @Valid private EvaluationCriteriaRequest evaluationCriteria = new EvaluationCriteriaRequest();
    private String promptTemplateId;
    @Size(max = 12000) private String promptOverride;
    private boolean active;

    @Data
    public static class EvaluationCriteriaRequest {
        @DecimalMin("0.0") @DecimalMax("1.0") private double grammarWeight = 0.30;
        @DecimalMin("0.0") @DecimalMax("1.0") private double vocabularyWeight = 0.30;
        @DecimalMin("0.0") @DecimalMax("1.0") private double politenessWeight = 0.25;
        @DecimalMin("0.0") @DecimalMax("1.0") private double taskCompletionWeight = 0.15;
    }
}
