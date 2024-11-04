/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

  private final JedisConnectionFactory connectionFactory;
  private final RedisStandaloneConfiguration connection;

  @Autowired
  public RedisConfig(
      @Value("${spring.data.redis.host}") String redisHost,
      @Value("${spring.data.redis.port}") Integer redisPort,
      @Value("${spring.data.redis.password}") String redisPassword) {

    this.connection = new RedisStandaloneConfiguration(redisHost, redisPort);
    connection.setDatabase(1);
    connection.setPassword(redisPassword);
    this.connectionFactory = new JedisConnectionFactory(connection);
  }

  /**
   * Configures the Java client for Redis.
   *
   * @return the Java Redis client object
   */

  /**
   * Provides the central class of the Redis module for Redis interactions.
   *
   * @return the Redis interaction class
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    if (!connectionFactory.isRunning()) connectionFactory.start();
    redisTemplate.setConnectionFactory(connectionFactory);
    return redisTemplate;
  }
}
