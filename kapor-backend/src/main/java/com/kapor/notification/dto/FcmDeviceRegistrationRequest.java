package com.kapor.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FcmDeviceRegistrationRequest {
    @NotBlank(message = "FCM token là bắt buộc")
    private String token;
    @Pattern(regexp = "android|ios", message = "Platform phải là android hoặc ios")
    private String platform;
    private Integer timezoneOffsetMinutes;
}
