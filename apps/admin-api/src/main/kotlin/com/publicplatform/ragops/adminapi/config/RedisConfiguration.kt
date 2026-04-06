package com.publicplatform.ragops.adminapi.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * RAGAS 이벤트 드리븐 평가용 Redis 설정.
 *
 * ragas.eval.redis.enabled=true 일 때만 활성화된다.
 * 테스트 환경(application-test.yml)에서는 false로 비활성화해 Redis 없이 테스트가 돌아간다.
 */
@Configuration
@ConditionalOnProperty(name = ["ragas.eval.redis.enabled"], havingValue = "true", matchIfMissing = false)
class RedisConfiguration {

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        return template
    }
}
