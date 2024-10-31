/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.config;

import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.redis.metadata.RedisMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.inbound.SftpStreamingMessageSource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(value = "app.source.use", havingValue = "sftp")
public class SftpConnectSource {

  private final SftpSourceConfig sftpSourceConfig;
  private final RedisMetadataStore metadataStore;

  @Autowired
  public SftpConnectSource(SftpSourceConfig sftpSourceConfig, RedisMetadataStore metadataStore) {
    this.sftpSourceConfig = sftpSourceConfig;
    this.metadataStore = metadataStore;
  }

  @Bean
  public DefaultSftpSessionFactory sftpSessionFactory() {
    var factory = new DefaultSftpSessionFactory(true);
    factory.setHost(sftpSourceConfig.url());
    factory.setPort(sftpSourceConfig.port());
    factory.setUser(sftpSourceConfig.username());
    // factory.setPassword("root");
    // client private key
    final Resource clientPrivateKey =
        StringUtils.hasText(sftpSourceConfig.accessKeyCertLocation())
            ? new PathResource(sftpSourceConfig.accessKeyCertLocation())
            : new ClassPathResource("keypair.pem");

    factory.setPrivateKey(clientPrivateKey);

    // we know our host :)
    factory.setAllowUnknownKeys(true);

    return factory;
  }

  @Bean
  public SftpRemoteFileTemplate sftpTemplate() {
    return new SftpRemoteFileTemplate(sftpSessionFactory());
  }

  /**
   * https://docs.spring.io/spring-integration/reference/sftp/streaming.html
   *
   * @return
   */
  @Bean
  public MessageSource<InputStream> ftpMessageSource() {
    SftpStreamingMessageSource messageSource = new SftpStreamingMessageSource(sftpTemplate());
    messageSource.setRemoteDirectory(sftpSourceConfig.downloadPath());
    messageSource.setFilter(
        new SftpPersistentAcceptOnceFileListFilter(metadataStore, sftpSourceConfig.downloadPath()));
    messageSource.setMaxFetchSize(-1);

    return messageSource;
  }
}
