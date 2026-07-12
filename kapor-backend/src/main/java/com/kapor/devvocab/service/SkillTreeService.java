package com.kapor.devvocab.service;

import com.kapor.analytics.model.LearningProgress;
import com.kapor.analytics.repository.LearningProgressRepository;
import com.kapor.devvocab.dto.SkillNodeDto;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.devvocab.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillTreeService {

    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final LearningProgressRepository progressRepository;

    public List<SkillNodeDto> getSkillTree(String userId, String domain) {
        List<Topic> topics = topicRepository.findByDomainAndIsActiveTrueOrderByOrderAsc(domain);
        
        List<LearningProgress> progressList = progressRepository.findByUserIdAndDomain(userId, domain);
        Map<String, LearningProgress> progressMap = progressList.stream()
                .collect(Collectors.toMap(LearningProgress::getTopicId, p -> p));

        return topics.stream().map(topic -> {
            LearningProgress progress = progressMap.get(topic.getId());
            long totalLessons = lessonRepository.countByTopicId(topic.getId());
            
            boolean isLocked = false;
            // Simplified prerequisite check: if previous nodes are not completed
            if (topic.getPrerequisiteTopicIds() != null && !topic.getPrerequisiteTopicIds().isEmpty()) {
                isLocked = topic.getPrerequisiteTopicIds().stream().anyMatch(prereqId -> {
                    LearningProgress prereqProgress = progressMap.get(prereqId);
                    return prereqProgress == null || prereqProgress.getCompletionPercent() < 100.0;
                });
            }

            return SkillNodeDto.builder()
                    .id(topic.getId())
                    .title(topic.getTitle())
                    .titleVi(topic.getTitleVi())
                    .domain(topic.getDomain())
                    .isLocked(isLocked)
                    .prerequisites(topic.getPrerequisiteTopicIds())
                    .completionPercent(progress != null ? progress.getCompletionPercent() : 0.0)
                    .totalLessons(totalLessons)
                    .completedLessons(progress != null ? progress.getCompletedLessons() : 0)
                    .build();
        }).collect(Collectors.toList());
    }
}
