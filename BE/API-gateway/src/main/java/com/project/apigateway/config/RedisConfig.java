package com.project.apigateway.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DnsResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

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

    @Value("${redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources() {
        return DefaultClientResources.builder()
                .ioThreadPoolSize(4) // Adjust based on your needs
                .computationThreadPoolSize(4) // Adjust based on your needs
                .dnsResolver(new CustomDnsResolver()) // Use IPv4 only resolver
                .build();
    }

    // Custom DNS resolver that prefers IPv4 addresses
    private static class CustomDnsResolver implements DnsResolver {
        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            // Filter for IPv4 addresses only if needed
            return addresses;
        }
    }

    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory(ClientResources clientResources) {
        // Direct URI-based configuration, closer to your working test
        RedisURI redisURI = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort)
                .withAuthentication(redisUsername, redisPassword)
                .withTimeout(Duration.ofMillis(connectionTimeout))
                .withSsl(sslEnabled)
                .build();

        // Socket options
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(socketTimeout))
                .keepAlive(true)
                .build();

        // Client options
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(socketTimeout)))
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build();

        // For testing, let's start with simpler configuration, no pooling
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientResources(clientResources)
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofMillis(connectionTimeout))
                .build();

        // Standard configuration
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisHost);
        serverConfig.setPort(redisPort);
        serverConfig.setUsername(redisUsername);
        serverConfig.setPassword(redisPassword);

        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(serverConfig, clientConfig);
        lettuceConnectionFactory.setValidateConnection(true);

        return lettuceConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

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

    // Add a connection test helper
    @Bean
    public RedisConnectionTester redisConnectionTester(LettuceConnectionFactory connectionFactory) {
        return new RedisConnectionTester(connectionFactory);
    }

    // Helper class to test connections at startup
    public static class RedisConnectionTester {
        private final LettuceConnectionFactory connectionFactory;

        public RedisConnectionTester(LettuceConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            testConnection();
        }

        private void testConnection() {
            try {
                System.out.println("Testing Redis connection...");
                String result = connectionFactory.getConnection().ping();
                System.out.println("Redis connection test result: " + result);
            } catch (Exception e) {
                System.err.println("Redis connection test failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}