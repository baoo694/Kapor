package com.kapor.analytics.service;

import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserRepository userRepository;

    public void updateStreakForUser(String userId) {
        User user = userRepository.findById(userId).orElseThrow();
        User.Streak streak = user.getStreak();
        LocalDate today = LocalDate.now();
        LocalDate lastActive = streak.getLastActiveDate();

        if (lastActive == null) {
            streak.setCurrent(1);
            streak.setLongest(1);
            streak.setLastActiveDate(today);
        } else if (lastActive.equals(today)) {
            // Already updated today
            return;
        } else if (lastActive.equals(today.minusDays(1))) {
            // Consecutive day
            streak.setCurrent(streak.getCurrent() + 1);
            if (streak.getCurrent() > streak.getLongest()) {
                streak.setLongest(streak.getCurrent());
            }
            streak.setLastActiveDate(today);
        } else {
            // Streak broken, check if freezes apply
            long daysMissed = ChronoUnit.DAYS.between(lastActive, today) - 1;
            if (streak.getFreezesRemaining() >= daysMissed) {
                // Use freezes to maintain streak
                streak.setFreezesRemaining(streak.getFreezesRemaining() - (int) daysMissed);
                streak.setCurrent(streak.getCurrent() + 1);
                if (streak.getCurrent() > streak.getLongest()) {
                    streak.setLongest(streak.getCurrent());
                }
            } else {
                // Streak reset
                streak.setCurrent(1);
            }
            streak.setLastActiveDate(today);
        }

        userRepository.save(user);
    }
}
