package com.accountservice.kafka;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, String> kafkaTemplate;

    @PostConstruct
    public void sendMessage() {
        kafkaTemplate.send("TESTING_TOPIC", "Ive sent test topic, take it");
    }
}
