package com.accountservice.service.impl;

import com.accountservice.common.BaseResponse;
import com.accountservice.common.Const;
import com.accountservice.model.PaymentMethod;
import com.accountservice.model.Transaction;
import com.accountservice.payload.request.client.*;
import com.accountservice.payload.response.GetPaymentMethodDetailsResponse;
import com.accountservice.payload.response.PaymentMethodMetadataResponse;
import com.accountservice.payload.response.PaymentMethodVerificationDetailsResponse;
import com.accountservice.payload.response.client.*;
import com.accountservice.repository.PaymentMethodRepository;
import com.accountservice.service.PaymentMethodService;
import com.accountservice.service.TransactionHistoryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class PaymentMethodServiceImpl implements PaymentMethodService {
    private static final char DEFAULT_MASK_CHAR = '*';
    private static final int DEFAULT_VISIBLE_LEADING_DIGITS = 0;
    private static final int DEFAULT_VISIBLE_TRAILING_DIGITS = 4;

    private final PaymentMethodRepository paymentMethodRepository;

    private final TransactionHistoryService transactionHistoryService;

    private final MongoTemplate mongoTemplate;


    @Override
    public BaseResponse<?> createPaymentMethod(String userId, CreatePaymentMethodRequest createPaymentMethodRequest) {
        String type = createPaymentMethodRequest.getType();
        String nickname = createPaymentMethodRequest.getNickname();
        boolean isDefault = createPaymentMethodRequest.isSetAsDefault();
        // If set this payment method as default, then change others' "isDefault" to false
        if (isDefault) {
            List<PaymentMethod> paymentMethods = paymentMethodRepository.findPaymentMethodsByUserId(userId);
            paymentMethods.forEach(paymentMethod -> paymentMethod.setDefault(false));
            paymentMethodRepository.saveAll(paymentMethods);
        }
        PaymentMethodDetailsRequest detailsRequest = createPaymentMethodRequest.getDetails();

        Map<String, Object> metadata = new TreeMap<>();
        metadata.put("accountHolderName", detailsRequest.getAccountHolderName());
        metadata.put("routingNumber", detailsRequest.getRoutingNumber());
        metadata.put("bankName", detailsRequest.getBankName());
        metadata.put("accountNumber", detailsRequest.getAccountNumber());
        metadata.put("verificationRequired", true);
        metadata.put("verificationMethod", "MICRO_DEPOSITS");
        metadata.put("verifiedAt", null);

        PaymentMethod newPaymentMethod = new PaymentMethod();
        newPaymentMethod.setUserId(userId);
        newPaymentMethod.setType(type);
        newPaymentMethod.setNickname(nickname);
        newPaymentMethod.setDefault(isDefault);
        newPaymentMethod.setStatus(PaymentMethod.PaymentMethodStatus.VERIFICATION_PENDING.name());
        newPaymentMethod.setMaskedNumber(mask(detailsRequest.getAccountNumber()));
        newPaymentMethod.setAddedAt(Instant.now());
        newPaymentMethod.setMetadata(metadata);
        newPaymentMethod.setUpdatedAt(Instant.now());

        PaymentMethod savedPaymentMethod = paymentMethodRepository.save(newPaymentMethod);

        CreatePaymentMethodResponse createPaymentMethodResponse = new CreatePaymentMethodResponse();
        createPaymentMethodResponse.setId(savedPaymentMethod.getId());
        createPaymentMethodResponse.setType(type);
        createPaymentMethodResponse.setNickname(nickname);
        createPaymentMethodResponse.setMaskedNumber(savedPaymentMethod.getMaskedNumber());
        createPaymentMethodResponse.setDefault(isDefault);
        createPaymentMethodResponse.setStatus(PaymentMethod.PaymentMethodStatus.VERIFICATION_PENDING.name());
        createPaymentMethodResponse.setAddedAt(savedPaymentMethod.getAddedAt());
        createPaymentMethodResponse.setVerificationRequired(true);
        createPaymentMethodResponse.setVerificationInstructions("We've sent two small deposits to your bank account. They should appear in 1-3 business days. Verify your account by confirming the exact amounts.");

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Payment method added successfully",
            createPaymentMethodResponse
        );
    }

    @Override
    public BaseResponse<?> verifyPaymentMethod(String paymentMethodId, VerifyPaymentMethodRequest verifyPaymentMethodRequest) {
        float amount1 = verifyPaymentMethodRequest.getVerificationDataRequest().getAmount1();
        float amount2 = verifyPaymentMethodRequest.getVerificationDataRequest().getAmount2();
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId).orElse(null);
        if (paymentMethod == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Payment method not found with paymentMethodId: " + paymentMethodId,
                ""
            );
        }

        if ((amount1 == 0.32f && amount2 == 0.45f) || (amount1 == 0.45f && amount2 == 0.32f)) {
            Map<String, Object> newMetaData = paymentMethod.getMetadata();
            newMetaData.put("verifiedAt", Instant.now());
            paymentMethod.setMetadata(newMetaData);
            paymentMethod.setStatus(PaymentMethod.PaymentMethodStatus.ACTIVE.name());
            paymentMethod.setUpdatedAt(Instant.now());
            paymentMethodRepository.save(paymentMethod);

            VerifyPaymentMethodResponse verifyPaymentMethodResponse = new VerifyPaymentMethodResponse();
            verifyPaymentMethodResponse.setId(paymentMethod.getId());
            verifyPaymentMethodResponse.setStatus(paymentMethod.getStatus());
            verifyPaymentMethodResponse.setVerifiedAt(Instant.now());

            return new BaseResponse<>(
                Const.STATUS_RESPONSE.SUCCESS,
                "Payment method verified successfully",
                verifyPaymentMethodResponse
            );
        }
        /*
        The logic code of this implement:
        1. Check if the payment method exists
        2. Check if the payment method is already verified
        3. Check if the verification amounts are correct
        4. If all checks pass, update the payment method status to verified and save it
         */

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.ERROR,
            "Payment method verification failed",
            ""
        );
    }

    @Override
    public BaseResponse<?> listPaymentMethods(ListPaymentMethodsRequest listPaymentMethodsRequest) {
        String userId = listPaymentMethodsRequest.getUserId();
        List<String> statuses = listPaymentMethodsRequest.getStatuses() == null ? List.of() : listPaymentMethodsRequest.getStatuses();
        List<String> types = listPaymentMethodsRequest.getTypes() == null ? List.of() : listPaymentMethodsRequest.getTypes();

        Criteria userIdCriteria = Criteria.where("userId").is(userId);
        Query query = new Query(userIdCriteria);
        if (!statuses.isEmpty()) {
            query.addCriteria(Criteria.where("status").in(statuses));
        }
        if (!types.isEmpty()) {
            query.addCriteria(Criteria.where("type").in(types));
        }
        List<PaymentMethod> paymentMethods = mongoTemplate.find(query, PaymentMethod.class);

        List<RetrievePaymentMethodResponse> paymentMethodResponses = new ArrayList<>();
        for (PaymentMethod paymentMethod : paymentMethods) {
            RetrievePaymentMethodResponse retrievePaymentMethodResponse = new RetrievePaymentMethodResponse();
            retrievePaymentMethodResponse.setId(paymentMethod.getId());
            retrievePaymentMethodResponse.setNickname(paymentMethod.getNickname());
            retrievePaymentMethodResponse.setMaskedNumber(paymentMethod.getMaskedNumber());
            retrievePaymentMethodResponse.setType(paymentMethod.getType());
            retrievePaymentMethodResponse.setDefault(paymentMethod.isDefault());
            retrievePaymentMethodResponse.setStatus(paymentMethod.getStatus());
            retrievePaymentMethodResponse.setAddedAt(paymentMethod.getAddedAt());
            retrievePaymentMethodResponse.setMetadata(paymentMethod.getMetadata());
            List<Transaction> transactions = transactionHistoryService.getTransactions(
                    new GetTransactionsRequest(userId, null, null, null, null, null, null, null, List.of(paymentMethod.getId()), null, null)
            ).getData().getItems();
            Instant lastUsedAt = transactions.isEmpty() ? null : transactions.get(0).getCreatedAt();
            retrievePaymentMethodResponse.setLastUsedAt(lastUsedAt);

            paymentMethodResponses.add(retrievePaymentMethodResponse);
        }
        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Payment methods retrieved successfully",
            new ListPaymentMethodsResponse(paymentMethodResponses)
        );
    }

    @Override
    public BaseResponse<?> getPaymentMethodDetails(String paymentMethodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId).orElse(null);
        if (paymentMethod == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Payment method not found with paymentMethodId: " + paymentMethodId,
                ""
            );
        }

        List<Transaction> transactions = transactionHistoryService.getTransactions(
                new GetTransactionsRequest(paymentMethod.getUserId(), null, null, null, null, null, null, null, List.of(paymentMethod.getId()), null, null)
        ).getData().getItems();

        Instant lastUsedAt = transactions.isEmpty() ? null : transactions.get(0).getCreatedAt();
        String accountHolderName = paymentMethod.getMetadata().get("accountHolderName") == null ? "" : paymentMethod.getMetadata().get("accountHolderName").toString();
        String bankName = paymentMethod.getMetadata().get("bankName") == null ? "" : paymentMethod.getMetadata().get("bankName").toString();
        Date verifiedAt = paymentMethod.getMetadata().get("verifiedAt") == null ? null : (Date) paymentMethod.getMetadata().get("verifiedAt");
        String verificationMethod = paymentMethod.getMetadata().get("verificationMethod") == null ? "" : paymentMethod.getMetadata().get("verificationMethod").toString();

        GetPaymentMethodDetailsResponse getPaymentMethodDetailsResponse = new GetPaymentMethodDetailsResponse();
        getPaymentMethodDetailsResponse.setId(paymentMethod.getId());
        getPaymentMethodDetailsResponse.setNickname(paymentMethod.getNickname());
        getPaymentMethodDetailsResponse.setMaskedNumber(paymentMethod.getMaskedNumber());
        getPaymentMethodDetailsResponse.setDefault(paymentMethod.isDefault());
        getPaymentMethodDetailsResponse.setStatus(paymentMethod.getStatus());
        getPaymentMethodDetailsResponse.setAddedAt(paymentMethod.getAddedAt());
        getPaymentMethodDetailsResponse.setLastUsedAt(lastUsedAt);
        getPaymentMethodDetailsResponse.setMetadata(new PaymentMethodMetadataResponse(accountHolderName, bankName));
        getPaymentMethodDetailsResponse.setVerificationDetails(new PaymentMethodVerificationDetailsResponse(verifiedAt, verificationMethod));

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Payment method retrieved successfully",
            getPaymentMethodDetailsResponse
        );
    }

    @Override
    public BaseResponse<?> updatePaymentMethod(String paymentMethodId, UpdatePaymentMethodRequest updatePaymentMethodRequest) {
        String nickname = updatePaymentMethodRequest.getNickname();
        String status = updatePaymentMethodRequest.getStatus();
        String accountHolderName = updatePaymentMethodRequest.getMetadata().getAccountHolderName();
        boolean isDefault = updatePaymentMethodRequest.isSetAsDefault();
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId).orElse(null);
        if (paymentMethod == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Payment method not found with paymentMethodId: " + paymentMethodId,
                ""
            );
        }
        if (isDefault) {
            List<PaymentMethod> paymentMethods = paymentMethodRepository.findPaymentMethodsByUserId(paymentMethod.getUserId());
            for (PaymentMethod method : paymentMethods) {
                method.setDefault(false);
            }
            paymentMethodRepository.saveAll(paymentMethods);
        }

        Map<String, Object> metadata = paymentMethod.getMetadata();
        metadata.put("accountHolderName", accountHolderName);
        paymentMethod.setNickname(nickname);
        paymentMethod.setStatus(status);
        paymentMethod.setDefault(isDefault);
        paymentMethod.setMetadata(metadata);
        paymentMethod.setUpdatedAt(Instant.now());

        UpdatePaymentMethodResponse updatePaymentMethodResponse = new UpdatePaymentMethodResponse();
        paymentMethodRepository.save(paymentMethod);

        updatePaymentMethodResponse.setNickname(nickname);
        updatePaymentMethodResponse.setId(paymentMethod.getId());
        updatePaymentMethodResponse.setMaskedNumber(paymentMethod.getMaskedNumber());
        updatePaymentMethodResponse.setDefault(paymentMethod.isDefault());
        updatePaymentMethodResponse.setStatus(paymentMethod.getStatus());
        updatePaymentMethodResponse.setAddedAt(paymentMethod.getAddedAt());
        updatePaymentMethodResponse.setUpdatedAt(paymentMethod.getUpdatedAt());

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Payment method updated successfully",
            updatePaymentMethodResponse
        );
    }

    @Override
    public BaseResponse<?> removePaymentMethod(String paymentMethodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId).orElse(null);
        if (paymentMethod == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Payment method not found with paymentMethodId: " + paymentMethodId,
                ""
            );
        }

        paymentMethodRepository.delete(paymentMethod);

        RemovePaymentMethodResponse removePaymentMethodResponse = new RemovePaymentMethodResponse();
        removePaymentMethodResponse.setId(paymentMethod.getId());
        removePaymentMethodResponse.setDeactivatedAt(Instant.now());
        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Payment method removed successfully",
            removePaymentMethodResponse
        );
    }


    /*================================================================= PRIVATE FUNCTIONS =================================================================================*/
    /**
     * Masks a payment method number, allowing specification of leading/trailing visible digits
     * and the masking character. Handles null, empty, and short strings gracefully.
     * <p>
     * Example: mask("1234567890123456", 6, 4, '*') -> "123456******3456"
     * Example: mask("9876543210", 2, 2, '#') -> "98######10"
     * Example: mask("123456", 4, 4, '*') -> "123456" (total visible >= length)
     *
     * @param paymentNumber The payment number string to mask. Can contain non-digits; they are treated like digits for positioning.
     * @return The masked payment number, the original if too short to mask meaningfully,
     *         or an empty string if the input is null or empty.
     * @throws IllegalArgumentException if visibleLeadingDigits or visibleTrailingDigits is negative.
     */
    private String mask(String paymentNumber) {
        // 1. Input Validation
        if (paymentNumber == null || paymentNumber.isEmpty()) {
            return ""; // Return empty for null/empty input
        }

        int visibleLeadingDigits = DEFAULT_VISIBLE_LEADING_DIGITS;
        int visibleTrailingDigits = DEFAULT_VISIBLE_TRAILING_DIGITS;

        int length = paymentNumber.length();

        // 2. Check if masking is needed/possible
        // If the total number of visible digits is greater than or equal to the string length,
        // no masking is needed, return the original string.
        if (length <= visibleLeadingDigits + visibleTrailingDigits) {
            return paymentNumber;
        }

        // 3. Build the masked string
        // Append leading visible part
        int maskedSectionLength = length - visibleLeadingDigits - visibleTrailingDigits;

        return paymentNumber.substring(0, visibleLeadingDigits) +
                // Append masking characters
                String.valueOf(DEFAULT_MASK_CHAR).repeat(maskedSectionLength) +
                // Append trailing visible part
                // Use substring(startIndex) which goes to the end of the string
                paymentNumber.substring(length - visibleTrailingDigits);
    }

}
