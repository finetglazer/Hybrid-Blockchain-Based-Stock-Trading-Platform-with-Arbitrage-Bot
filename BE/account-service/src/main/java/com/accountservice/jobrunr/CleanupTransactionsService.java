package com.accountservice.jobrunr;

import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional
public class CleanupTransactionsService {
    private final MongoTemplate mongoTemplate;

    @Scheduled(cron = "0 0 7 * * ?") // Every day at 7 am
    public void clearTransactionsOfPreviousDay() {

    }
}
