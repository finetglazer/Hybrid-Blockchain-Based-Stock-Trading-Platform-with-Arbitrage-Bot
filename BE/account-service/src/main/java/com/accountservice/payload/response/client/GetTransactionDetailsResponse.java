package com.accountservice.payload.response.client;

import com.accountservice.model.Transaction;
import com.accountservice.payload.response.GetTransactionDetailsAccountInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTransactionDetailsResponse {
    private Transaction transaction;
    private GetTransactionDetailsAccountInfo account;
    private GetTransactionDetailsPaymentMethodInfo paymentMethod;
}
