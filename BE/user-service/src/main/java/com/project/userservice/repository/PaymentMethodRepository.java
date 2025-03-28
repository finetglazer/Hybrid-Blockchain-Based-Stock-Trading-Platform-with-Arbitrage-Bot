package com.project.userservice.repository;

import com.project.userservice.model.PaymentMethod;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends MongoRepository<PaymentMethod, String> {
    Optional<PaymentMethod> findByUserId(String userId);
}
