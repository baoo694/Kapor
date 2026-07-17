package com.kapor.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kapor.devvocab.model.Topic;
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
public class TopicDto {

    private String id;

    @NotBlank(message = "Domain is required")
    private String domain;

    @NotBlank(message = "Korean title is required")
    private String title;

    @NotBlank(message = "Vietnamese title is required")
    private String titleVi;

    private String description;

    @Min(value = 0, message = "Order must be zero or greater")
    private int order;

    @Builder.Default
    private List<String> prerequisiteTopicIds = new ArrayList<>();

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @JsonProperty("isActive")
    @JsonAlias("active")
    @Builder.Default
    private Boolean isActive = true;

    public static TopicDto fromEntity(Topic topic) {
        return TopicDto.builder()
                .id(topic.getId())
                .domain(topic.getDomain())
                .title(topic.getTitle())
                .titleVi(topic.getTitleVi())
                .description(topic.getDescription())
                .order(topic.getOrder())
                .prerequisiteTopicIds(topic.getPrerequisiteTopicIds() == null
                        ? new ArrayList<>() : new ArrayList<>(topic.getPrerequisiteTopicIds()))
                .tags(topic.getTags() == null ? new ArrayList<>() : new ArrayList<>(topic.getTags()))
                .isActive(topic.isActive())
                .build();
    }
}
