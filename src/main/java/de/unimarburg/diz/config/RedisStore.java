/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.redis.metadata.RedisMetadataStore;
import org.springframework.stereotype.Component;

@Component
public class RedisStore {

  RedisConfig redisConfig;

  @Autowired
  public RedisStore(RedisConfig redisConfig) {
    this.redisConfig = redisConfig;
  }

  @Bean
  public RedisMetadataStore metaStore() {
    return new RedisMetadataStore(redisConfig.redisTemplate());
  }
}
