package com.accountservice.service.impl;

import com.accountservice.common.BaseResponse;
import com.accountservice.model.PaymentMethod;
import com.accountservice.payload.request.client.CreatePaymentMethodRequest;
import com.accountservice.payload.request.client.PaymentMethodDetailsRequest;
import com.accountservice.service.PaymentMethodService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class PaymentMethodServiceImpl implements PaymentMethodService {
//    @Override
//    public BaseResponse<?> createPaymentMethod(String userId, CreatePaymentMethodRequest createPaymentMethodRequest) {
//        String type = createPaymentMethodRequest.getType();
//        String nickname = createPaymentMethodRequest.getNickname();
//        boolean isDefault = createPaymentMethodRequest.isDefault();
//        PaymentMethodDetailsRequest detailsRequest = createPaymentMethodRequest.getDetails();
//        Map<String, Object> detailsMap = new TreeMap<>();
//
//        PaymentMethod newPaymentMethod = new PaymentMethod();
//        newPaymentMethod.setUserId(userId);
//        newPaymentMethod.setType(type);
//        newPaymentMethod.setNickname(nickname);
//        newPaymentMethod.setDefault(isDefault);
//        newPaymentMethod.setStatus(PaymentMethod.PaymentMethodStatus.ACTIVE.name());
//        newPaymentMethod.setMaskedNumber();
//    }

}
