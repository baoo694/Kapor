package com.kapor.admin.controller;

import com.kapor.admin.model.AdminPrompt;
import com.kapor.admin.service.AdminContentService;
import com.kapor.common.dto.ApiResponse;
import com.kapor.dictionary.model.DictionaryEntry;
import com.kapor.pronunciation.model.PronunciationExercise;
import com.kapor.techtalk.model.TechTalkScenario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminContentController {
    private final AdminContentService service;

    @GetMapping("/scenarios") public ResponseEntity<ApiResponse<List<TechTalkScenario>>> scenarios() { return ResponseEntity.ok(ApiResponse.ok(service.scenarios())); }
    @PostMapping("/scenarios") public ResponseEntity<ApiResponse<TechTalkScenario>> createScenario(@RequestBody TechTalkScenario value) { return ResponseEntity.ok(ApiResponse.ok(service.createScenario(value))); }
    @PutMapping("/scenarios/{id}") public ResponseEntity<ApiResponse<TechTalkScenario>> updateScenario(@PathVariable String id, @RequestBody TechTalkScenario value) { return ResponseEntity.ok(ApiResponse.ok(service.updateScenario(id, value))); }
    @DeleteMapping("/scenarios/{id}") public ResponseEntity<ApiResponse<Void>> deleteScenario(@PathVariable String id) { service.deleteScenario(id); return ResponseEntity.ok(ApiResponse.ok(null)); }

    @GetMapping("/pronunciation-exercises") public ResponseEntity<ApiResponse<List<PronunciationExercise>>> pronunciation() { return ResponseEntity.ok(ApiResponse.ok(service.pronunciationExercises())); }
    @PostMapping("/pronunciation-exercises") public ResponseEntity<ApiResponse<PronunciationExercise>> createPronunciation(@RequestBody PronunciationExercise value) { return ResponseEntity.ok(ApiResponse.ok(service.createPronunciationExercise(value))); }
    @PutMapping("/pronunciation-exercises/{id}") public ResponseEntity<ApiResponse<PronunciationExercise>> updatePronunciation(@PathVariable String id, @RequestBody PronunciationExercise value) { return ResponseEntity.ok(ApiResponse.ok(service.updatePronunciationExercise(id, value))); }
    @DeleteMapping("/pronunciation-exercises/{id}") public ResponseEntity<ApiResponse<Void>> deletePronunciation(@PathVariable String id) { service.deletePronunciationExercise(id); return ResponseEntity.ok(ApiResponse.ok(null)); }

    @GetMapping("/dictionary") public ResponseEntity<ApiResponse<List<DictionaryEntry>>> dictionary(@RequestParam(required = false) String query) { return ResponseEntity.ok(ApiResponse.ok(service.dictionary(query))); }
    @PostMapping("/dictionary") public ResponseEntity<ApiResponse<DictionaryEntry>> createDictionary(@RequestBody DictionaryEntry value) { return ResponseEntity.ok(ApiResponse.ok(service.createDictionaryEntry(value))); }
    @PostMapping("/dictionary/import") public ResponseEntity<ApiResponse<List<DictionaryEntry>>> importDictionary(@RequestBody List<DictionaryEntry> values) { return ResponseEntity.ok(ApiResponse.ok(service.importDictionary(values))); }
    @PutMapping("/dictionary/{id}") public ResponseEntity<ApiResponse<DictionaryEntry>> updateDictionary(@PathVariable String id, @RequestBody DictionaryEntry value) { return ResponseEntity.ok(ApiResponse.ok(service.updateDictionaryEntry(id, value))); }
    @DeleteMapping("/dictionary/{id}") public ResponseEntity<ApiResponse<Void>> deleteDictionary(@PathVariable String id) { service.deleteDictionaryEntry(id); return ResponseEntity.ok(ApiResponse.ok(null)); }

    @GetMapping("/prompts") public ResponseEntity<ApiResponse<List<AdminPrompt>>> prompts() { return ResponseEntity.ok(ApiResponse.ok(service.prompts())); }
    @PostMapping("/prompts") public ResponseEntity<ApiResponse<AdminPrompt>> createPrompt(@RequestBody AdminPrompt value) { return ResponseEntity.ok(ApiResponse.ok(service.createPrompt(value))); }
    @PutMapping("/prompts/{id}") public ResponseEntity<ApiResponse<AdminPrompt>> updatePrompt(@PathVariable String id, @RequestBody AdminPrompt value) { return ResponseEntity.ok(ApiResponse.ok(service.updatePrompt(id, value))); }
    @DeleteMapping("/prompts/{id}") public ResponseEntity<ApiResponse<Void>> deletePrompt(@PathVariable String id) { service.deletePrompt(id); return ResponseEntity.ok(ApiResponse.ok(null)); }
}
