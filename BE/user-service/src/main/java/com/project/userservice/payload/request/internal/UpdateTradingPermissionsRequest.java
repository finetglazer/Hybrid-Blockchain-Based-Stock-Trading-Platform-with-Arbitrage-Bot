package com.project.userservice.payload.request.internal;

import com.project.userservice.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTradingPermissionsRequest {
    private List<String> permissions;
    private String reason;
    private String requestingService;
}
