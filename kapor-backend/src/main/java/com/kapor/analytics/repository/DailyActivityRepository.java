package com.kapor.analytics.repository;

import com.kapor.analytics.model.DailyActivity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyActivityRepository extends MongoRepository<DailyActivity, String> {

    Optional<DailyActivity> findByUserIdAndDate(String userId, LocalDate date);

    List<DailyActivity> findByUserIdAndDateBetweenOrderByDateAsc(String userId, LocalDate startDate, LocalDate endDate);

    long countByDate(LocalDate date);
}
