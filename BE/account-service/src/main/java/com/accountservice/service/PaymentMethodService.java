package com.accountservice.service;

import com.accountservice.common.BaseResponse;
import com.accountservice.payload.request.client.CreatePaymentMethodRequest;
import com.accountservice.payload.request.client.ListPaymentMethodsRequest;
import com.accountservice.payload.request.client.UpdatePaymentMethodRequest;
import com.accountservice.payload.request.client.VerifyPaymentMethodRequest;

public interface PaymentMethodService {
    BaseResponse<?> createPaymentMethod(String userId, CreatePaymentMethodRequest createPaymentMethodRequest);
    BaseResponse<?> verifyPaymentMethod(String paymentMethodId, VerifyPaymentMethodRequest verifyPaymentMethodRequest);
    BaseResponse<?> listPaymentMethods(ListPaymentMethodsRequest listPaymentMethodsRequest);
    BaseResponse<?> getPaymentMethodDetails(String paymentMethodId);
    BaseResponse<?> updatePaymentMethod(String paymentMethodId, UpdatePaymentMethodRequest updatePaymentMethodRequest);
    BaseResponse<?> removePaymentMethod(String paymentMethodId);
}
