package phonemarket.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import phonemarket.cache.CacheNames;

import java.time.Duration;
import java.util.Map;

/**
 * Redis 只负责缓存，MySQL 仍然是最终数据来源。
 *
 * roomDetails：房间页面轮询频繁，缓存 10 秒。
 * activeGames：主页参与中比赛列表，缓存 30 秒。
 *
 * 当前项目为单服务器部署，不引入分布式锁组件。
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory
    ) {
        RedisSerializationContext.SerializationPair<Object> valueSerialization =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new JdkSerializationRedisSerializer()
                );

        RedisCacheConfiguration defaults =
                RedisCacheConfiguration.defaultCacheConfig()
                        .disableCachingNullValues()
                        .computePrefixWith(
                                cacheName -> "phonemarket:" + cacheName + "::"
                        )
                        .serializeValuesWith(valueSerialization)
                        .entryTtl(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                CacheNames.ROOM_DETAILS,
                defaults.entryTtl(Duration.ofSeconds(10)),

                CacheNames.ACTIVE_GAMES,
                defaults.entryTtl(Duration.ofSeconds(30))
        );

        return RedisCacheManager
                .builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(cacheConfigurations)
                // 缓存写入和删除在数据库事务提交后执行。
                .transactionAware()
                .build();
    }
}
