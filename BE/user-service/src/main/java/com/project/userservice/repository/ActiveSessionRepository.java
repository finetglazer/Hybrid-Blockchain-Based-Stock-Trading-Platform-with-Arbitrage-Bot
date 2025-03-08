package com.project.userservice.repository;

import com.project.userservice.model.ActiveSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActiveSessionRepository extends MongoRepository<ActiveSession, String> {
    Optional<ActiveSession> findByUserId(String userId);
    void deleteByUserId(String userId);
}
