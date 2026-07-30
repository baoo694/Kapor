package com.kapor.admin.service;

import com.kapor.admin.model.AdminPrompt;
import com.kapor.admin.repository.AdminPromptRepository;
import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.dictionary.model.DictionaryEntry;
import com.kapor.dictionary.repository.DictionaryEntryRepository;
import com.kapor.pronunciation.model.PronunciationExercise;
import com.kapor.pronunciation.repository.PronunciationExerciseRepository;
import com.kapor.techtalk.model.TechTalkScenario;
import com.kapor.techtalk.repository.TechTalkScenarioRepository;
import com.kapor.techtalk.repository.RoleplaySessionRepository;
import com.kapor.techtalk.dto.AdminScenarioRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminContentService {
    private final TechTalkScenarioRepository scenarioRepository;
    private final RoleplaySessionRepository roleplaySessionRepository;
    private final PronunciationExerciseRepository pronunciationRepository;
    private final DictionaryEntryRepository dictionaryRepository;
    private final AdminPromptRepository promptRepository;

    public List<TechTalkScenario> scenarios() { return scenarioRepository.findAll(); }
    public TechTalkScenario createScenario(AdminScenarioRequest request) {
        TechTalkScenario value = scenario(request, null);
        value.setCreatedAt(Instant.now());
        return scenarioRepository.save(value);
    }
    public TechTalkScenario updateScenario(String id, AdminScenarioRequest request) {
        TechTalkScenario existing = scenarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Scenario", "id", id));
        TechTalkScenario value = scenario(request, existing);
        return scenarioRepository.save(value);
    }
    public void deleteScenario(String id) {
        TechTalkScenario existing = scenarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario", "id", id));
        if (roleplaySessionRepository.existsByScenarioId(id)) {
            existing.setActive(false);
            scenarioRepository.save(existing);
        } else {
            scenarioRepository.delete(existing);
        }
    }

    public List<PronunciationExercise> pronunciationExercises() { return pronunciationRepository.findAllByOrderByOrderAsc(); }
    public PronunciationExercise createPronunciationExercise(PronunciationExercise value) {
        value.setId(null); value.setCreatedAt(Instant.now()); return pronunciationRepository.save(value);
    }
    public PronunciationExercise updatePronunciationExercise(String id, PronunciationExercise value) {
        PronunciationExercise existing = pronunciationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pronunciation exercise", "id", id));
        value.setId(existing.getId()); value.setCreatedAt(existing.getCreatedAt()); return pronunciationRepository.save(value);
    }
    public void deletePronunciationExercise(String id) { pronunciationRepository.deleteById(id); }

    public List<DictionaryEntry> dictionary(String query) {
        return query == null || query.isBlank() ? dictionaryRepository.findAll().stream().sorted(java.util.Comparator.comparing(DictionaryEntry::getKorean, java.util.Comparator.nullsLast(String::compareTo))).toList()
                : dictionaryRepository.findByKoreanContainingIgnoreCaseOrVietnameseContainingIgnoreCaseOrderByKoreanAsc(query.trim(), query.trim());
    }
    public DictionaryEntry createDictionaryEntry(DictionaryEntry value) { value.setId(null); return dictionaryRepository.save(value); }
    public DictionaryEntry updateDictionaryEntry(String id, DictionaryEntry value) {
        DictionaryEntry existing = dictionaryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dictionary entry", "id", id));
        value.setId(existing.getId()); value.setSearchCount(existing.getSearchCount()); return dictionaryRepository.save(value);
    }
    public void deleteDictionaryEntry(String id) { dictionaryRepository.deleteById(id); }
    public List<DictionaryEntry> importDictionary(List<DictionaryEntry> values) {
        values.forEach(value -> value.setId(null)); return dictionaryRepository.saveAll(values);
    }

    public List<AdminPrompt> prompts() {
        return promptRepository.findAll().stream()
                .sorted(java.util.Comparator
                        .comparing(AdminPrompt::getKey, java.util.Comparator.nullsLast(String::compareTo))
                        .thenComparing(AdminPrompt::getPromptVersion,
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
    }
    public AdminPrompt createPrompt(AdminPrompt value) {
        value.setId(null);
        if (value.getKey() == null || value.getKey().isBlank()) value.setKey("techtalk.roleplay.system");
        int nextVersion = promptRepository.findByKeyOrderByPromptVersionDesc(value.getKey()).stream()
                .map(AdminPrompt::getPromptVersion).filter(java.util.Objects::nonNull).max(Integer::compareTo).orElse(0) + 1;
        value.setPromptVersion(nextVersion);
        value.setStatus("draft");
        validatePrompt(value);
        return promptRepository.save(value);
    }
    public AdminPrompt updatePrompt(String id, AdminPrompt value) {
        AdminPrompt existing = promptRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Admin prompt", "id", id));
        if (!"draft".equals(existing.getStatus())) {
            throw new IllegalArgumentException("Published and archived prompts are immutable. Clone the prompt to create a draft.");
        }
        value.setId(existing.getId());
        value.setKey(existing.getKey());
        value.setPromptVersion(existing.getPromptVersion());
        value.setStatus(existing.getStatus());
        value.setCreatedAt(existing.getCreatedAt());
        validatePrompt(value);
        return promptRepository.save(value);
    }
    public void deletePrompt(String id) {
        AdminPrompt prompt = promptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin prompt", "id", id));
        if ("published".equals(prompt.getStatus())) {
            throw new IllegalArgumentException("Published prompts cannot be deleted. Publish another version first.");
        }
        if (scenarioRepository.existsByPromptTemplateId(id)) {
            throw new IllegalArgumentException("Prompt versions referenced by a scenario cannot be deleted.");
        }
        promptRepository.delete(prompt);
    }

    public AdminPrompt clonePrompt(String id) {
        AdminPrompt existing = promptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin prompt", "id", id));
        return createPrompt(AdminPrompt.builder().key(existing.getKey()).name(existing.getName())
                .description(existing.getDescription()).content(existing.getContent())
                .requiredPlaceholders(new java.util.ArrayList<>(existing.getRequiredPlaceholders() == null
                        ? java.util.List.of() : existing.getRequiredPlaceholders())).build());
    }

    public AdminPrompt publishPrompt(String id) {
        AdminPrompt target = promptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin prompt", "id", id));
        validatePrompt(target);
        if ("published".equals(target.getStatus())) return target;
        promptRepository.findFirstByKeyAndStatusOrderByPromptVersionDesc(target.getKey(), "published")
                .ifPresent(current -> {
                    current.setStatus("archived");
                    promptRepository.save(current);
                });
        target.setStatus("published");
        return promptRepository.save(target);
    }

    private TechTalkScenario scenario(AdminScenarioRequest request, TechTalkScenario existing) {
        if (request.getPersona().getName() == null || request.getPersona().getName().isBlank()
                || request.getPersona().getRole() == null || request.getPersona().getRole().isBlank()) {
            throw new IllegalArgumentException("Persona name and role are required");
        }
        if (!java.util.Set.of("beginner", "intermediate", "advanced").contains(request.getDifficulty())) {
            throw new IllegalArgumentException("Difficulty must be beginner, intermediate, or advanced");
        }
        if (request.getPromptTemplateId() != null && !request.getPromptTemplateId().isBlank()) {
            AdminPrompt prompt = promptRepository.findById(request.getPromptTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Admin prompt", "id", request.getPromptTemplateId()));
            if (!java.util.Set.of("published", "archived").contains(prompt.getStatus())) {
                throw new IllegalArgumentException("Scenarios can only reference an immutable prompt version");
            }
        }
        double total = request.getEvaluationCriteria().getGrammarWeight()
                + request.getEvaluationCriteria().getVocabularyWeight()
                + request.getEvaluationCriteria().getPolitenessWeight()
                + request.getEvaluationCriteria().getTaskCompletionWeight();
        if (Math.abs(total - 1.0) > 0.001) {
            throw new IllegalArgumentException("Evaluation criteria weights must add up to 1.0");
        }
        TechTalkScenario.Mission mission = request.getMission() == null ? new TechTalkScenario.Mission() : request.getMission();
        if ((mission.getTitleVi() == null || mission.getTitleVi().isBlank()) && request.getMissionVi() != null) {
            mission.setTitleVi(request.getMissionVi());
        }
        if ((mission.getRequiredVocabulary() == null || mission.getRequiredVocabulary().isEmpty())) {
            mission.setRequiredVocabulary(new java.util.ArrayList<>(request.getRequiredVocabulary()));
        }
        TechTalkScenario value = TechTalkScenario.builder()
                .id(existing == null ? null : existing.getId())
                .title(request.getTitle().trim())
                .titleVi(request.getTitleVi().trim())
                .domain(request.getDomain().trim())
                .difficulty(request.getDifficulty().trim())
                .order(Math.max(0, request.getOrder()))
                .persona(request.getPersona())
                .missionVi(request.getMissionVi())
                .mission(mission)
                .objectives(new java.util.ArrayList<>(request.getObjectives()))
                .requiredVocabulary(new java.util.ArrayList<>(request.getRequiredVocabulary()))
                .evaluationCriteria(TechTalkScenario.EvaluationCriteria.builder()
                        .grammarWeight(request.getEvaluationCriteria().getGrammarWeight())
                        .vocabularyWeight(request.getEvaluationCriteria().getVocabularyWeight())
                        .politenessWeight(request.getEvaluationCriteria().getPolitenessWeight())
                        .taskCompletionWeight(request.getEvaluationCriteria().getTaskCompletionWeight())
                        .build())
                .promptTemplateId(request.getPromptTemplateId())
                .promptOverride(request.getPromptOverride())
                .active(request.isActive())
                .schemaVersion(2)
                .createdAt(existing == null ? null : existing.getCreatedAt())
                .version(existing == null ? null : existing.getVersion())
                .build();
        return value;
    }

    private void validatePrompt(AdminPrompt value) {
        if (value.getContent() == null || value.getContent().isBlank()) {
            throw new IllegalArgumentException("Prompt content is required");
        }
        if (value.getRequiredPlaceholders() == null) value.setRequiredPlaceholders(new java.util.ArrayList<>());
        for (String placeholder : value.getRequiredPlaceholders()) {
            if (!value.getContent().contains("{{" + placeholder + "}}")) {
                throw new IllegalArgumentException("Prompt is missing required placeholder {{" + placeholder + "}}");
            }
        }
    }
}
