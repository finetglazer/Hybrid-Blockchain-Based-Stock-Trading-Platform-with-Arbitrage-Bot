//// src/main/java/com/stocktrading/marketdata/config/MongoConfig.java
//package com.stocktrading.marketdata.config;
//
//import org.bson.types.Decimal128;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
//
//import java.math.BigDecimal;
//import java.util.Arrays;
//
//@Configuration
//public class MongoConfig {
//
//    @Bean
//    public MongoCustomConversions mongoCustomConversions() {
//        return new MongoCustomConversions(Arrays.asList(
//                new BigDecimalToDecimal128Converter(),
//                new Decimal128ToBigDecimalConverter()
//        ));
//    }
//
//    private static class BigDecimalToDecimal128Converter implements Converter<BigDecimal, Decimal128> {
//        @Override
//        public Decimal128 convert(BigDecimal source) {
//            return new Decimal128(source);
//        }
//    }
//
//    private static class Decimal128ToBigDecimalConverter implements Converter<Decimal128, BigDecimal> {
//        @Override
//        public BigDecimal convert(Decimal128 source) {
//            return source.bigDecimalValue();
//        }
//    }
//}