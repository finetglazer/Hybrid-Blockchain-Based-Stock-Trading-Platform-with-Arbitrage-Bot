package com.project.userservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    private String id;

    @Indexed
    private String email;

    private String token;

    private Instant createdAt = Instant.now();

    // Automatically expire documents 1 hour after 'expiresAt'
    @Indexed
    private Instant expiresAt;
}
