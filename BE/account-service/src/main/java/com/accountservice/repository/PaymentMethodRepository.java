package com.accountservice.repository;

import com.accountservice.model.PaymentMethod;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends MongoRepository<PaymentMethod, String> {
    List<PaymentMethod> findPaymentMethodsByUserId(String userId);
    Optional<PaymentMethod> findPaymentMethodById(String id);
}
