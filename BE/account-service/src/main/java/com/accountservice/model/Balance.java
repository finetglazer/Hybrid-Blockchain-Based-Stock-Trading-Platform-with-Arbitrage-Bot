package com.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "balance")
public class Balance {
    @Id
    private String id;

    private String accountId;
    private Float total;
    private String currency;
    private Float available;
    private Float reserved;
    private Instant updatedAt;
}
