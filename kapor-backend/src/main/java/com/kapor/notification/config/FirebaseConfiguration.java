package com.kapor.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
    FirebaseApp firebaseApp(@Value("${firebase.service-account-file:}") String serviceAccountFile) throws IOException {
        if (serviceAccountFile.isBlank()) {
            throw new IllegalStateException("FIREBASE_SERVICE_ACCOUNT_FILE is required when FIREBASE_ENABLED=true");
        }
        try (FileInputStream input = new FileInputStream(serviceAccountFile)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(input))
                    .build();
            return FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
        }
    }
}
