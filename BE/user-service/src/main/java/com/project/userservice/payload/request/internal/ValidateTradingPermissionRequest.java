package com.project.userservice.payload.request.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidateTradingPermissionRequest {
    private String requiredPermission;
    private String requestingService;
}
