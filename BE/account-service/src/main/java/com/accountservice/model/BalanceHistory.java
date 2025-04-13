package com.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "balance_history")
public class BalanceHistory {
    @Id
    private String id;

    private Date date;
    private String accountId;
    private String userId;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal deposits;
    private BigDecimal withdrawals;
    private BigDecimal tradesNet;
    private BigDecimal fees;
}
