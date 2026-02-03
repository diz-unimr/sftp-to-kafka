/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.config;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.redis.metadata.RedisMetadataStore;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

@Log4j2
// @Configuration
// @EnableConfigurationProperties(value = {SftpSourceConfig.class})
// @EnableIntegration
@Configuration
public class IntegrationTargets {

  private static String TARGET_TOPIC = "TEST-TOPICS-FTP";

  private final KafkaTemplate<String, String> kafkaTemplate;

  private final SourceConfig sourceConfig;

  private final KafkaProducerConfig kafkaCfg;
  private final RedisMetadataStore metaStore;
  private static final String LAST_MODIFIED = "LAST_MODIFIED";

  public IntegrationTargets(
      @Value("${app.target.topic}") String targetTopic,
      KafkaProducerConfig kafkaCfg,
      SourceConfig sourceConfig,
      RedisMetadataStore metaStore) {
    this.kafkaCfg = kafkaCfg;
    this.kafkaTemplate = kafkaCfg.kafkaTemplate();
    this.metaStore = metaStore;
    this.sourceConfig = sourceConfig;
    if (!StringUtils.hasText(targetTopic)) {
      throw new InvalidConfigurationException(
          "target output topic must be set! please check application configuration 'app.target.topic'");
    }
    TARGET_TOPIC = targetTopic;
  }

  public IntegrationFlow StreamFlowToKafka(MessageChannel source) {
    return IntegrationFlow.from(source)
        .handle((payload, headers) -> getMessageWithEnrichedLastModifiedHeader(payload, headers))
        .handle(
            (payload, headers) -> {
              final String kafkaMessageKey = getKafkaMessageKey(payload, headers);

              final String s = metaStore.get(kafkaMessageKey);

              if (s != null
                  && s.equals(Objects.requireNonNull(headers.get(LAST_MODIFIED)).toString())) {
                log.info(
                    "skipping message id '{}' since it has been processed before with timestamp {}",
                    headers.getId(),
                    headers.get("LAST_MODIFIED"));
                return null;
              }

              final CompletableFuture<SendResult<String, String>> send =
                  kafkaTemplate.send(
                      TARGET_TOPIC,
                      null,
                      Objects.requireNonNull(headers.getTimestamp()),
                      kafkaMessageKey,
                      payload.toString());
              send.whenComplete(
                  (res, ex) -> {
                    if (ex != null) {
                      log.error("storing message at KAFKA failed", ex.getCause());
                      throw new RuntimeException("could not store message into Kafka");
                    }
                    metaStore.put(
                        kafkaMessageKey,
                        Objects.requireNonNull(headers.get("LAST_MODIFIED")).toString());

                    log.info(
                        "Message SUCCESSFULLY STORED AT KAFKA !{}", res.getProducerRecord().key());
                  });
              return null;
            })
        .get();
  }

  private static String gzipPayload(String json, UUID messageId) {
    try {
      final var baos = new ByteArrayOutputStream();
      final var gzipOutputStream = new GZIPOutputStream(baos);
      gzipOutputStream.write(json.getBytes(StandardCharsets.UTF_8));
      gzipOutputStream.close();
      return baos.toString();
    } catch (Exception e) {
      log.error("Cannot compress payload for message id '{}'", messageId);
      return "";
    }
  }

  private static Message<Object> getMessageWithEnrichedLastModifiedHeader(
      Object message, MessageHeaders headers) {
    try {
      Message<Object> lastModified = null;
      if (headers.containsKey("file_originalFile")) {
        final File fileOrigFile = new File(headers.get("file_originalFile").toString());

        BasicFileAttributes attr =
            Files.readAttributes(fileOrigFile.toPath(), BasicFileAttributes.class);
        lastModified = buildMessageWithLastModifiedHeader(message, attr.lastModifiedTime());
        lastModified.getHeaders().putAll(headers);
        return lastModified;
      } else if (headers.containsKey("file_remoteFileInfo")) {
        var modifiedValue =
            new JSONObject(headers.get("file_remoteFileInfo").toString()).get("modified");
        lastModified = buildMessageWithLastModifiedHeader(message, modifiedValue);
        lastModified.getHeaders().putAll(headers);
      }
      return lastModified;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Message<Object> buildMessageWithLastModifiedHeader(
      Object message, Object lastModified) {
    var builder =
        MutableMessageBuilder.withPayload(message).setHeader("LAST_MODIFIED", lastModified);
    return builder.build();
  }

  private String getKafkaMessageKey(Object payload, MessageHeaders headers) {
    String kafkaMessageKey;
    if (StringUtils.hasText(sourceConfig.propertyAsId())) {
      kafkaMessageKey = tryGetPayloadInternalId(payload.toString());
    } else {
      kafkaMessageKey = Objects.requireNonNull(headers.getId()).toString();
    }
    return kafkaMessageKey;
  }

  private String tryGetPayloadInternalId(String messageAsString) {

    var idFromJsonPath = JsonUtil.getPropValue(sourceConfig.propertyAsId(), messageAsString);
    if (idFromJsonPath == null) {
      throw new JSONException(
          "element id from Json with jsonpath " + sourceConfig.propertyAsId() + " is missing! ");
    }

    return idFromJsonPath.toString();
  }
}
