package com.accountservice.repository;

import com.accountservice.model.ReservationRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReservationRecordRepository extends MongoRepository<ReservationRecord, String> {

}
