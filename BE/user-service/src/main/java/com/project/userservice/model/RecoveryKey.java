package com.project.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "recovery_keys")
public class RecoveryKey {
    @Id
    private String id;

    private String userId;
    private String keyHash;  // Store a hash of the key, not the plaintext
    private Boolean used = false;
    private Instant createdAt = Instant.now();
    private Instant usedAt;
}

