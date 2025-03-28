package com.project.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "paymentMethods")
public class PaymentMethod {
    @Id
    private String id;

    @Field("id")
    private String paymentMethodId;
    private String userId;
    private String nickname;
    private Map<String, Object> metadata;
    private boolean isDefault;
    private String type;
    private String status;
    private String maskedNumber;
    private Instant addedAt;
    private Instant updatedAt;
}
