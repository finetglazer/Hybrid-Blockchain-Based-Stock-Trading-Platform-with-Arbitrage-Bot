package com.project.apigateway;


import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

public class RedisConnectionTest {
    public static void main(String[] args) {
        RedisURI redisURI = RedisURI.builder()
                .withHost("redis-14694.c258.us-east-1-4.ec2.redns.redis-cloud.com")
                .withPort(14694)
                .withSsl(false) // Change this to false
                .withAuthentication("default", "1Pox3Zq8mQuUsAvzKlvjdCOJE3xxxprJ")
                .build();

        RedisClient redisClient = RedisClient.create(redisURI);
        redisClient.setOptions(ClientOptions.builder()
                .build());

        try {
            System.out.println("Connecting to Redis...");
            StatefulRedisConnection<String, String> connection = redisClient.connect();
            System.out.println("Connected successfully!");

            String pong = connection.sync().ping();
            System.out.println("Ping response: " + pong);

            connection.close();
        } catch (Exception e) {
            System.err.println("Failed to connect to Redis: " + e.getMessage());
            e.printStackTrace();
        } finally {
            redisClient.shutdown();
        }
    }
}