package com.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "balance_history")
public class BalanceHistory {
    @Id
    private String id;

    private LocalDate date;
    private String accountId;
    private String userId;
    private Float openingBalance;
    private Float closingBalance;
    private Float deposits;
    private Float withdrawals;
    private Float tradesNet;
    private Float fees;
}
