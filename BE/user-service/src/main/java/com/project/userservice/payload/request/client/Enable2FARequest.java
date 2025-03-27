package com.project.userservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Enable2FARequest {
    private String type; // SMS_CODE, etc.
    private String phoneNumber;
}

//example: phone: 0365706735 in vietnam
/*
{
    "type": "SMS_CODE",
    "phoneNumber": "0365706735"
}


 */