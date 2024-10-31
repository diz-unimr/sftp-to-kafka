/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

@Configuration
@EnableIntegration
@Log4j2
@EnableConfigurationProperties(value = {SourceConfig.class})
public class IntegrationMaster {

  private final IntegrationFlowContext intFlowctx;
  private final IntegrationSources integrationSources;
  private final IntegrationTargets integrationTargets;
  private final Optional<MessageSource<InputStream>> ftpMessageSource;
  private final SourceConfig sourceConfig;

  private List<IntegrationFlowRegistration> currentlyRegistered = new ArrayList<>();

  @Autowired
  public IntegrationMaster(
      IntegrationFlowContext intFlowCtx,
      IntegrationSources integrationSources,
      IntegrationTargets integrationTargets,
      Optional<MessageSource<InputStream>> ftpMessageSource,
      SourceConfig sourceConfig) {

    this.intFlowctx = intFlowCtx;
    this.integrationSources = integrationSources;
    this.integrationTargets = integrationTargets;
    this.ftpMessageSource = ftpMessageSource;
    this.sourceConfig = sourceConfig;

    var resultChannel = MessageChannels.direct(IntegrationSources.MSG_CHANNEL_INPUT).getObject();

    var processMessageRegistration =
        intFlowCtx.registration(processMessages(resultChannel)).register();
    currentlyRegistered.add(processMessageRegistration);

    final boolean isSourceLocalFiles =
        StringUtils.hasText(sourceConfig.use()) && sourceConfig.use().equalsIgnoreCase("local");
    final boolean isSourceSftp =
        StringUtils.hasText(sourceConfig.use()) && sourceConfig.use().equalsIgnoreCase("sftp");

    if (isSourceLocalFiles) {
      var registrationTest =
          intFlowCtx
              .registration(integrationSources.readFromLocalFolder(sourceConfig.localPath()))
              .id("readTestData")
              .useFlowIdAsPrefix()
              .register();
      currentlyRegistered.add(registrationTest);
    } else {
      if (isSourceSftp) {
        assert ftpMessageSource.isPresent();
        var sftpRegistration =
            intFlowCtx
                .registration(integrationSources.readFromSftp(ftpMessageSource.get()))
                .id("readFromSftp")
                .useFlowIdAsPrefix()
                .register();
        currentlyRegistered.add(sftpRegistration);
      } else {
        log.info("configured source is unknown, will do nothing. :)");
      }
    }
  }

  public IntegrationFlow processMessages(MessageChannel channel) {
    return integrationTargets.StreamFlowToKafka(channel);
  }
}
