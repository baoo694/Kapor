package com.kapor.notification.service;

import com.kapor.analytics.model.DailyActivity;
import com.kapor.analytics.repository.DailyActivityRepository;
import com.kapor.notification.model.FcmDeviceToken;
import com.kapor.notification.repository.FcmDeviceTokenRepository;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Sends at most one daily reminder per active device after the configured time. */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyGoalReminderJob {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final UserRepository userRepository;
    private final DailyActivityRepository activityRepository;
    private final FcmDeviceService deviceService;
    private final FcmDeviceTokenRepository deviceRepository;
    private final FcmPushService pushService;

    @Scheduled(cron = "${firebase.reminder-cron:0 */5 * * * *}")
    public void remindLearners() {
        for (User user : userRepository.findAll()) {
            if (user.getSettings() == null || !user.getSettings().isNotificationsEnabled()
                    || user.getSettings().getReminderTime() == null || user.getSettings().getReminderTime().isBlank()) {
                continue;
            }
            LocalTime reminderTime;
            try {
                reminderTime = LocalTime.parse(user.getSettings().getReminderTime(), TIME);
            } catch (DateTimeParseException exception) {
                log.warn("Ignoring invalid reminder time for user {}", user.getId());
                continue;
            }
            for (FcmDeviceToken device : deviceService.activeDevices(user.getId())) {
                remindIfNeeded(user, device, reminderTime);
            }
        }
    }

    private void remindIfNeeded(User user, FcmDeviceToken device, LocalTime reminderTime) {
        int offset = device.getTimezoneOffsetMinutes() == null ? 0 : device.getTimezoneOffsetMinutes();
        LocalDateTime now = Instant.now().atOffset(ZoneOffset.ofTotalSeconds(offset * 60)).toLocalDateTime();
        LocalDate date = now.toLocalDate();
        if (now.toLocalTime().isBefore(reminderTime) || date.equals(device.getLastReminderDate())) return;

        DailyActivity activity = activityRepository.findByUserIdAndDate(user.getId(), date).orElse(null);
        int studied = activity == null ? 0 : activity.getMinutesStudied();
        int target = Math.max(1, user.getSettings().getDailyGoalMinutes());
        if (studied >= target) return;

        boolean sent = pushService.send(device, "Nhắc học hôm nay", "Bạn đã học " + studied + "/" + target
                + " phút. Hoàn thành thêm một hoạt động để đạt mục tiêu nhé!", Map.of("route", "/dashboard"));
        if (sent) {
            device.setLastReminderDate(date);
            deviceRepository.save(device);
        }
    }
}
