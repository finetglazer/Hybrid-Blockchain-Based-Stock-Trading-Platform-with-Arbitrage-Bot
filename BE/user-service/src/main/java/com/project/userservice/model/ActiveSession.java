package com.project.userservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "active_sessions")
public class ActiveSession {
    @Id
    private String id;

    private String userId;      // Reference to the user who logged in
    private String ipAddress;   // IP address from which the user logged in
    private Instant loginTime;  // When the login occurred
    private String token;       // The JWT (or session token) issued upon login
}
