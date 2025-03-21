package com.project.apigateway.config;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
//exclude auto configuration
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class})
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName("redis-14694.c258.us-east-1-4.ec2.redns.redis-cloud.com");
        configuration.setPort(14694);
        configuration.setUsername("default");
        configuration.setPassword("1Pox3Zq8mQuUsAvzKlvjdCOJE3xxxprJ");

        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(configuration);

        // Explicitly disable SSL
        lettuceConnectionFactory.setUseSsl(false);

        // Additional configuration for connection validation
        lettuceConnectionFactory.setValidateConnection(true);

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
}