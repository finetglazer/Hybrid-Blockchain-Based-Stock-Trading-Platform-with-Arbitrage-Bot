package com.stocktrading.marketdata.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "portfolios")
public class Portfolio {
    @Id
    private String id;

    private String accountId;
    private String userId;
    private Instant createdAt;
    private String name;
    private List<PositionInfo> positions;
    private Instant updatedAt;
}
