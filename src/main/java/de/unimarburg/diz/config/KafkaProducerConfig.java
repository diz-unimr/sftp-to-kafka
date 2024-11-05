/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

  private final ProducerFactory producerFactory;

  @Autowired
  public KafkaProducerConfig(ProducerFactory producerFactory) {
    this.producerFactory = producerFactory;
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate() {
    final KafkaTemplate<String, String> stringStringKafkaTemplate =
        new KafkaTemplate<>(producerFactory);

    return stringStringKafkaTemplate;
  }
}
