package com.project.userservice.repository;

import com.project.userservice.model.RecoveryKey;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecoveryKeyRepository extends MongoRepository<RecoveryKey, String> {
    List<RecoveryKey> findByUserIdAndUsed(String userId, Boolean used);
    Optional<RecoveryKey> findByKeyHashAndUsed(String keyHash, Boolean used);
}