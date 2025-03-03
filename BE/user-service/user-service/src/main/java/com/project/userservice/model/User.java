package com.project.userservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    private String coinbaseAccessToken;
    private String coinbaseRefreshToken;
    private Instant coinbaseExpiresAt;
}
