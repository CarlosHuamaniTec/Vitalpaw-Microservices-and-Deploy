package com.vitalpaw.sensoralert.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.admin-sdk-path}")
    private String firebaseCredentialsPath;

    @PostConstruct
    public void initialize() {
        try (FileInputStream serviceAccount = new FileInputStream(firebaseCredentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("FirebaseApp inicializado correctamente");
            }
        } catch (IOException e) {
            logger.error("Error al inicializar Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo inicializar Firebase", e);
        }
    }
}