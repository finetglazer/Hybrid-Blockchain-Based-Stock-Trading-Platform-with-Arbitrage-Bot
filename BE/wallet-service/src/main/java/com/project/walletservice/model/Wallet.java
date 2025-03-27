package com.project.walletservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wallets")
public class Wallet {
    @Id
    private String id;
    private String name;
    private String userId;
    private String networkId;
    private DefaultAddress defaultAddress;
    private FeatureSet featureSet;
    private String serverSignerStatus;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
