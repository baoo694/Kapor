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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminContentService {
    private final TechTalkScenarioRepository scenarioRepository;
    private final PronunciationExerciseRepository pronunciationRepository;
    private final DictionaryEntryRepository dictionaryRepository;
    private final AdminPromptRepository promptRepository;

    public List<TechTalkScenario> scenarios() { return scenarioRepository.findAll(); }
    public TechTalkScenario createScenario(TechTalkScenario value) {
        value.setId(null);
        value.setCreatedAt(Instant.now());
        return scenarioRepository.save(value);
    }
    public TechTalkScenario updateScenario(String id, TechTalkScenario value) {
        TechTalkScenario existing = scenarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Scenario", "id", id));
        value.setId(existing.getId()); value.setCreatedAt(existing.getCreatedAt());
        return scenarioRepository.save(value);
    }
    public void deleteScenario(String id) { scenarioRepository.deleteById(id); }

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

    public List<AdminPrompt> prompts() { return promptRepository.findAll(); }
    public AdminPrompt createPrompt(AdminPrompt value) { value.setId(null); return promptRepository.save(value); }
    public AdminPrompt updatePrompt(String id, AdminPrompt value) {
        AdminPrompt existing = promptRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Admin prompt", "id", id));
        value.setId(existing.getId()); return promptRepository.save(value);
    }
    public void deletePrompt(String id) { promptRepository.deleteById(id); }
}
