package com.stocktrading.kafka.scheduler;

import com.stocktrading.kafka.service.DepositSagaService;
import com.stocktrading.kafka.service.WithdrawalSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for saga timeout checking
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SagaTimeoutScheduler {
    
    private final DepositSagaService depositSagaService;
    private final WithdrawalSagaService withdrawalSagaService;
    
    /**
     * Check for timed-out sagas every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void checkForTimeouts() {
        log.debug("Running scheduled timeout check");
        
        try {
            depositSagaService.checkForTimeouts();
        } catch (Exception e) {
            log.error("Error during scheduled timeout check", e);
        }
    }
}
