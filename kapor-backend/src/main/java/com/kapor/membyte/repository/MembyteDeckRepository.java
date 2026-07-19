package com.kapor.membyte.repository;

import com.kapor.membyte.model.MembyteDeck;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembyteDeckRepository extends MongoRepository<MembyteDeck, String> {
    List<MembyteDeck> findByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<MembyteDeck> findByUserIdAndLessonId(String userId, String lessonId);
}
