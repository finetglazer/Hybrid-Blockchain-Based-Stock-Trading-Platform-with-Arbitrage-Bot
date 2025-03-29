package com.project.userservice.payload.response.client;

import com.project.userservice.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTradingPermissionsResponse {
    private String userId;
    private List<String> permissions;
    private Instant updatedAt;
}
