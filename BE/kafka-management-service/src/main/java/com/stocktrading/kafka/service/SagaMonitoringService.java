package com.stocktrading.kafka.service;

import com.stocktrading.kafka.model.DepositSagaState;
import com.stocktrading.kafka.model.enums.SagaStatus;
import com.stocktrading.kafka.repository.DepositSagaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for monitoring saga metrics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaMonitoringService {
    
    private final DepositSagaRepository depositSagaRepository;
    private final MeterRegistry meterRegistry;
    
    // Metrics counters
    private AtomicInteger activeSagasGauge;
    private AtomicInteger completedSagasCounter;
    private AtomicInteger failedSagasCounter;
    private Timer sagaExecutionTimer;
    
    @PostConstruct
    public void init() {
        // Initialize metrics
        activeSagasGauge = meterRegistry.gauge("saga.active.count", new AtomicInteger(0));
        
        completedSagasCounter = new AtomicInteger(0);
        meterRegistry.gauge("saga.completed.count", completedSagasCounter);
        
        failedSagasCounter = new AtomicInteger(0);
        meterRegistry.gauge("saga.failed.count", failedSagasCounter);
        
        sagaExecutionTimer = Timer.builder("saga.execution.time")
            .description("Time taken to complete sagas")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
        
        // Initial update
        updateMetrics();
    }
    
    /**
     * Update metrics every minute
     */
    @Scheduled(fixedRate = 60000)
    public void updateMetrics() {
        try {
            log.debug("Updating saga metrics");
            
            // Count active sagas
            List<SagaStatus> activeStatuses = Arrays.asList(
                SagaStatus.STARTED, SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING);
            
            List<DepositSagaState> activeSagas = depositSagaRepository.findByStatusIn(activeStatuses);
            activeSagasGauge.set(activeSagas.size());
            
            // Count completed and failed sagas in the last hour
            Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
            
            int completedCount = 0;
            int failedCount = 0;
            
            List<DepositSagaState> recentSagas = depositSagaRepository.findAll(); // In a real app, add a date filter
            
            for (DepositSagaState saga : recentSagas) {
                if (saga.getEndTime() == null) {
                    continue;
                }
                
                if (saga.getEndTime().isAfter(oneHourAgo)) {
                    if (saga.getStatus() == SagaStatus.COMPLETED) {
                        completedCount++;
                        
                        // Record execution time
                        long executionTime = Duration.between(saga.getStartTime(), saga.getEndTime()).toMillis();
                        sagaExecutionTimer.record(executionTime, TimeUnit.MILLISECONDS);
                        
                    } else if (saga.getStatus() == SagaStatus.FAILED || 
                              saga.getStatus() == SagaStatus.COMPENSATION_COMPLETED) {
                        failedCount++;
                    }
                }
            }
            
            completedSagasCounter.set(completedCount);
            failedSagasCounter.set(failedCount);
            
            log.debug("Metrics updated: active={}, completed={}, failed={}", 
                activeSagas.size(), completedCount, failedCount);
                
        } catch (Exception e) {
            log.error("Error updating saga metrics", e);
        }
    }
}
