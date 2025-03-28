package com.project.apigateway.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;

@Configuration
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class})
public class RedisConfig {

    @Value("${redis.host:redis-14694.c258.us-east-1-4.ec2.redns.redis-cloud.com}")
    private String redisHost;

    @Value("${redis.port:14694}")
    private int redisPort;

    @Value("${redis.username:default}")
    private String redisUsername;

    @Value("${redis.password:1Pox3Zq8mQuUsAvzKlvjdCOJE3xxxprJ}")
    private String redisPassword;

    @Value("${redis.connection.timeout:3000}")
    private long connectionTimeout;

    @Value("${redis.socket.timeout:3000}")
    private long socketTimeout;

    @Value("${redis.pool.max-active:8}")
    private int maxActive;

    @Value("${redis.pool.max-idle:8}")
    private int maxIdle;

    @Value("${redis.pool.min-idle:2}")
    private int minIdle;

    @Value("${redis.pool.max-wait:1000}")
    private long maxWait;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisHost);
        configuration.setPort(redisPort);
        configuration.setUsername(redisUsername);
        configuration.setPassword(redisPassword);

        // Connection Pool Configuration
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWait(Duration.ofMillis(maxWait));

        // Test connections when idle to prevent stale connections
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setMinEvictableIdleTime(Duration.ofSeconds(60));

        // Configure Lettuce client
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(socketTimeout))
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(socketTimeout)))
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build();

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(connectionTimeout))
                .poolConfig(poolConfig)
                .clientOptions(clientOptions)
                .build();

        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(configuration, clientConfig);
        lettuceConnectionFactory.setValidateConnection(true);
        lettuceConnectionFactory.setShareNativeConnection(false); // Disable connection sharing for better isolation

        return lettuceConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());

        // Use StringRedisSerializer for both keys and values
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer serializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> serializationContext = RedisSerializationContext
                .<String, String>newSerializationContext()
                .key(serializer)
                .value(serializer)
                .hashKey(serializer)
                .hashValue(serializer)
                .build();

        return new ReactiveStringRedisTemplate(factory, serializationContext);
    }
}