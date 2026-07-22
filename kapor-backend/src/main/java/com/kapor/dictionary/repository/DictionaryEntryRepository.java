package com.kapor.dictionary.repository;

import com.kapor.dictionary.model.DictionaryEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DictionaryEntryRepository extends MongoRepository<DictionaryEntry, String> {
    List<DictionaryEntry> findByKoreanContainingIgnoreCaseOrVietnameseContainingIgnoreCaseOrderByKoreanAsc(String korean, String vietnamese);
}
