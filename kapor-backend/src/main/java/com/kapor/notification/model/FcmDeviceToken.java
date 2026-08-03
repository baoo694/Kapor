package com.kapor.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fcm_device_tokens")
public class FcmDeviceToken {
    @Id private String id;
    private String userId;
    @Indexed(unique = true) private String token;
    private String platform;
    private Integer timezoneOffsetMinutes;
    @Builder.Default private boolean enabled = true;
    private LocalDate lastReminderDate;
    private Instant lastSeenAt;
}
