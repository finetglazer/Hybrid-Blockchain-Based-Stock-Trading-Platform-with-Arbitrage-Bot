//package com.project.userservice.model.kafka;
//
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//import lombok.experimental.SuperBuilder;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Data
//@SuperBuilder
//@NoArgsConstructor
//@AllArgsConstructor
//@EqualsAndHashCode(callSuper = true)
//public class EventMessage extends BaseMessage {
//    private Map<String, Object> payload = new HashMap<>();
//    private Boolean success = true;
//    private String errorCode;
//    private String errorMessage;
//
//    /**
//     * Helper method to get payload value
//     */
//    @SuppressWarnings("unchecked")
//    public <T> T getPayloadValue(String key) {
//        if (payload == null) {
//            return null;
//        }
//        return (T) payload.get(key);
//    }
//
//    /**
//     * Helper method to set payload value
//     */
//    public void setPayloadValue(String key, Object value) {
//        if (payload == null) {
//            payload = new HashMap<>();
//        }
//        payload.put(key, value);
//    }
//}