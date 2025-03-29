package com.project.userservice.payload.response.internal;

import com.project.userservice.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTradingPermissionResponse {
    private String userId;
    private boolean hasPermission;
    private List<String> actualPermissions;
    private String permissionRequesting;
}
