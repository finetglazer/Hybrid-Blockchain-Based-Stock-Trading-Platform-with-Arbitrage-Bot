package com.project.userservice.repository;

import com.project.userservice.model.SecurityVerification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecurityVerificationRepository extends MongoRepository<SecurityVerification, String> {
    Optional<SecurityVerification> findByIdAndStatus(String id, SecurityVerification.VerificationStatus status);
    Optional<SecurityVerification> findByUserId(String userId);
}
