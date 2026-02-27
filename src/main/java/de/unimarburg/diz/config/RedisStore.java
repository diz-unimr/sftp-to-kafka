/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.redis.metadata.RedisMetadataStore;

@Configuration
public class RedisStore {

  @Bean
  RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {

    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    return template;
  }

  @Bean
  public RedisMetadataStore metaStore(RedisTemplate<String, Object> redisTemplate) {
    return new RedisMetadataStore(redisTemplate);
  }
}
