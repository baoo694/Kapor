package com.kapor.notification.service;

import com.kapor.notification.dto.FcmDeviceRegistrationRequest;
import com.kapor.notification.model.FcmDeviceToken;
import com.kapor.notification.repository.FcmDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FcmDeviceService {
    private final FcmDeviceTokenRepository repository;

    public void register(String userId, FcmDeviceRegistrationRequest request) {
        FcmDeviceToken device = repository.findByToken(request.getToken()).orElseGet(FcmDeviceToken::new);
        device.setUserId(userId);
        device.setToken(request.getToken());
        device.setPlatform(request.getPlatform());
        device.setTimezoneOffsetMinutes(validOffset(request.getTimezoneOffsetMinutes()));
        device.setEnabled(true);
        device.setLastSeenAt(Instant.now());
        repository.save(device);
    }

    public void unregister(String userId, String token) {
        repository.findByToken(token).filter(device -> userId.equals(device.getUserId())).ifPresent(device -> {
            device.setEnabled(false);
            repository.save(device);
        });
    }

    public List<FcmDeviceToken> activeDevices(String userId) {
        return repository.findByUserIdAndEnabledTrue(userId);
    }

    private Integer validOffset(Integer value) {
        return value != null && value >= -18 * 60 && value <= 18 * 60 ? value : null;
    }
}
