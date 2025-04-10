//package com.project.userservice.model.kafka;
//
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import lombok.experimental.SuperBuilder;
//
//import java.time.Instant;
//
//@Data
//@SuperBuilder
//@NoArgsConstructor
//@AllArgsConstructor
//public abstract class BaseMessage {
//    private String messageId;
//    private String sagaId;
//    private Integer stepId;
//    private String type;
//    private Instant timestamp;
//    private String sourceService;
//
//    // Add this field to match the schema used by the Saga Orchestrator
//    private Integer version;
//}