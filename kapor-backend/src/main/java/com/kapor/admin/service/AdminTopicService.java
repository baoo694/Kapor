package com.kapor.admin.service;

import com.kapor.admin.dto.TopicDto;
import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.devvocab.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminTopicService {

    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;

    public List<TopicDto> findAll(String domain) {
        List<Topic> topics = domain == null || domain.isBlank()
                ? topicRepository.findAllByOrderByOrderAsc()
                : topicRepository.findByDomainOrderByOrderAsc(domain.trim());

        return topics.stream().map(TopicDto::fromEntity).toList();
    }

    public TopicDto save(TopicDto dto) {
        Topic topic = new Topic();
        apply(dto, topic);
        return TopicDto.fromEntity(topicRepository.save(topic));
    }

    public TopicDto update(String id, TopicDto dto) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        apply(dto, topic);
        return TopicDto.fromEntity(topicRepository.save(topic));
    }

    public void deleteById(String id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        if (lessonRepository.countByTopicId(id) > 0) {
            throw new IllegalArgumentException("Cannot delete a topic that still has lessons");
        }
        topicRepository.delete(topic);
    }

    private void apply(TopicDto dto, Topic topic) {
        topic.setDomain(dto.getDomain().trim());
        topic.setTitle(dto.getTitle().trim());
        topic.setTitleVi(dto.getTitleVi().trim());
        topic.setDescription(dto.getDescription() == null ? null : dto.getDescription().trim());
        topic.setOrder(dto.getOrder());
        topic.setPrerequisiteTopicIds(cleanList(dto.getPrerequisiteTopicIds()));
        topic.setTags(cleanList(dto.getTags()));
        topic.setActive(!Boolean.FALSE.equals(dto.getIsActive()));
    }

    private List<String> cleanList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
}
