package com.project.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "verification_tokens")
public class VerificationToken {
    @Id
    private String id;

    private String email;
    private String username;

    // Store the hashed password so we can later create the User without re-asking for the password.
    private String passwordHash;

    // The random token string the user will click in their email
    private String token;

    private Instant createdAt = Instant.now();

    // We set expiresAt (e.g., 24 hours in the future). After that, the token doc is invalid.
    @Indexed
    private Instant expiresAt;
}
