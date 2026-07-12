package com.kapor.analytics.service;

import com.kapor.TestDataFactory;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StreakService}.
 * Tests streak calculation logic: first day, consecutive days, streak breaks, and freeze protection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Streak Service Unit Tests")
class StreakServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StreakService streakService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createTestUser();
        testUser.setId("user_123");
    }

    @Nested
    @DisplayName("First Day Activity")
    class FirstDayActivity {

        @Test
        @DisplayName("should initialize streak to 1 on first ever activity")
        void shouldInitStreakOnFirstActivity() {
            testUser.getStreak().setLastActiveDate(null);
            when(userRepository.findById("user_123")).thenReturn(Optional.of(testUser));

            streakService.updateStreakForUser("user_123");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getStreak().getCurrent()).isEqualTo(1);
            assertThat(savedUser.getStreak().getLongest()).isEqualTo(1);
            assertThat(savedUser.getStreak().getLastActiveDate()).isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("Same Day Activity")
    class SameDayActivity {

        @Test
        @DisplayName("should not modify streak if already active today")
        void shouldNotModifyStreakIfAlreadyActiveToday() {
            testUser.getStreak().setCurrent(5);
            testUser.getStreak().setLongest(10);
            testUser.getStreak().setLastActiveDate(LocalDate.now());
            when(userRepository.findById("user_123")).thenReturn(Optional.of(testUser));

            streakService.updateStreakForUser("user_123");

            // Should not save since nothing changed
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Consecutive Day Streak")
    class ConsecutiveDayStreak {

        @Test
        @DisplayName("should increment streak on consecutive day")
        void shouldIncrementStreakOnConsecutiveDay() {
            testUser.getStreak().setCurrent(5);
            testUser.getStreak().setLongest(10);
            testUser.getStreak().setLastActiveDate(LocalDate.now().minusDays(1));
            when(userRepository.findById("user_123")).thenReturn(Optional.of(testUser));

            streakService.updateStreakForUser("user_123");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User saved = userCaptor.getValue();
            assertThat(saved.getStreak().getCurrent()).isEqualTo(6);
            assertThat(saved.getStreak().getLongest()).isEqualTo(10); // unchanged
        }

        @Test
        @DisplayName("should update longest streak when current exceeds it")
        void shouldUpdateLongestStreakWhenExceeded() {
            testUser.getStreak().setCurrent(10);
            testUser.getStreak().setLongest(10);
            testUser.getStreak().setLastActiveDate(LocalDate.now().minusDays(1));
            when(userRepository.findById("user_123")).thenReturn(Optional.of(testUser));

            streakService.updateStreakForUser("user_123");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User saved = userCaptor.getValue();
            assertThat(saved.getStreak().getCurrent()).isEqualTo(11);
            assertThat(saved.getStreak().getLongest()).isEqualTo(11);
        }
    }

    @Nested
    @DisplayName("Streak Break")
    class StreakBreak {

        @Test
        @DisplayName("should reset streak when more than 1 day missed without freeze")
        void shouldResetStreakWithoutFreeze() {
            testUser.getStreak().setCurrent(15);
            testUser.getStreak().setLongest(20);
            testUser.getStreak().setLastActiveDate(LocalDate.now().minusDays(3)); // 2 days missed
            testUser.getStreak().setFreezesRemaining(0); // No freezes
            when(userRepository.findById("user_123")).thenReturn(Optional.of(testUser));

            streakService.updateStreakForUser("user_123");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User saved = userCaptor.getValue();
            assertThat(saved.getStreak().getCurrent()).isEqualTo(1); // Reset
            assertThat(saved.getStreak().getLongest()).isEqualTo(20); // Unchanged
        }

        @Test
        @DisplayName("should use freeze to maintain streak if available")
        void shouldUseFreezeToMaintainStreak() {
            testUser.getStreak().setCurrent(15);
            testUser.getStreak().setLongest(20);
            testUser.getStreak().setLastActiveDate(LocalDate.now().minusDays(2)); // 1 day missed
            testUser.getStreak().setFreezesRemaining(2);
            when(userRepository.findById("user_123")).thenReturn(Optional.of(testUser));

            streakService.updateStreakForUser("user_123");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User saved = userCaptor.getValue();
            assertThat(saved.getStreak().getCurrent()).isEqualTo(16); // Continued
            assertThat(saved.getStreak().getFreezesRemaining()).isEqualTo(1); // 1 freeze used
        }

        @Test
        @DisplayName("should not use freeze when days missed exceeds freezes available")
        void shouldNotUseFreezeWhenInsufficientFreezes() {
            testUser.getStreak().setCurrent(10);
            testUser.getStreak().setLongest(10);
            testUser.getStreak().setLastActiveDate(LocalDate.now().minusDays(4)); // 3 days missed
            testUser.getStreak().setFreezesRemaining(2); // Only 2 freezes, need 3
            when(userRepository.findById("user_123")).thenReturn(Optional.of(testUser));

            streakService.updateStreakForUser("user_123");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User saved = userCaptor.getValue();
            assertThat(saved.getStreak().getCurrent()).isEqualTo(1); // Reset
        }
    }
}
