//package com.stocktrading.kafka.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
//import org.springframework.kafka.listener.KafkaMessageListenerContainer;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/kafka")
//@RequiredArgsConstructor
//public class KafkaAdminController {
//
//    private final KafkaListenerEndpointRegistry registry;
//
//    @PostMapping("/reset-offsets")
//    public ResponseEntity<String> resetOffsetsForAllListeners() {
//        registry.getListenerContainers().forEach(container -> {
//            String id = container.getListenerId();
//            log.info("Stopping container: {}", id);
//            container.stop();
//
//            if (container instanceof KafkaMessageListenerContainer) {
//                KafkaMessageListenerContainer<?, ?> kafkaContainer =
//                        (KafkaMessageListenerContainer<?, ?>) container;
//
//                kafkaContainer.getContainerProperties().setOnPartitionsAssigned(
//                        partitions -> {
//                            log.info("Seeking to end for {} partitions in listener: {}",
//                                    partitions.size(), id);
//                            kafkaContainer.getConsumer().seekToEnd(partitions);
//                        }
//                );
//            }
//
//            log.info("Starting container: {}", id);
//            container.start();
//        });
//
//        return ResponseEntity.ok("Reset offsets for all listeners");
//    }
//}
