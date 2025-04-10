//package com.stocktrading.kafka.config;
//
//import java.util.List;
//
//import com.fasterxml.jackson.databind.JavaType;
//import com.fasterxml.jackson.databind.type.TypeFactory;
//import com.fasterxml.jackson.databind.util.Converter;
//import org.bson.types.Decimal128;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
//import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//
//@Configuration
//public class MongoConfig extends AbstractMongoClientConfiguration {
//
//    @Override
//    protected String getDatabaseName() {
//        return "OrchestratorServiceDB";
//    }
//
//    @Override
//    public MongoCustomConversions customConversions() {
//        List<Converter<?, ?>> converters = new ArrayList<>();
//        converters.add(new BigDecimalDecimal128Converter());
//        converters.add(new Decimal128BigDecimalConverter());
//        return new MongoCustomConversions(converters);
//    }
//
//    private static class BigDecimalDecimal128Converter implements Converter<BigDecimal, Decimal128> {
//        @Override
//        public Decimal128 convert(BigDecimal source) {
//            return new Decimal128(source);
//        }
//
//        @Override
//        public JavaType getInputType(TypeFactory typeFactory) {
//            return null;
//        }
//
//        @Override
//        public JavaType getOutputType(TypeFactory typeFactory) {
//            return null;
//        }
//    }
//
//    private static class Decimal128BigDecimalConverter implements Converter<Decimal128, BigDecimal> {
//        @Override
//        public BigDecimal convert(Decimal128 source) {
//            return source.bigDecimalValue();
//        }
//
//        @Override
//        public JavaType getInputType(TypeFactory typeFactory) {
//            return null;
//        }
//
//        @Override
//        public JavaType getOutputType(TypeFactory typeFactory) {
//            return null;
//        }
//    }
//}