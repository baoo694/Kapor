package com.kapor.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.kapor.notification.model.FcmDeviceToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushService {
    private final Optional<FirebaseApp> firebaseApp;

    public boolean send(FcmDeviceToken device, String title, String body, Map<String, String> data) {
        if (firebaseApp.isEmpty()) return false;
        try {
            Message message = Message.builder().setToken(device.getToken())
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data).build();
            FirebaseMessaging.getInstance(firebaseApp.get()).send(message);
            return true;
        } catch (Exception exception) {
            log.warn("Could not send FCM notification to device {}", device.getId(), exception);
            return false;
        }
    }
}
