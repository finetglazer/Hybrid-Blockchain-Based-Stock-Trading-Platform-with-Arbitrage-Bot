package com.accountservice.kafka;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaConsumerService {

    @KafkaListener(topics = "TESTING_TOPIC", groupId = "account-service-consumer")
    public void consume(String message) {
        log.error("Message receivedddddd: {}", message);
    }
}
