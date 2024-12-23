/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.TreeMap;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.integration.zip.transformer.UnZipTransformer;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@EnableConfigurationProperties(value = {SftpSourceConfig.class})
public class IntegrationSources {

  public static final String MSG_CHANNEL_INPUT = "MSG_INPUT";
  private final SourceConfig sourceConfig;

  @Autowired
  public IntegrationSources(SourceConfig sourceConfig) {
    this.sourceConfig = sourceConfig;
  }

  public IntegrationFlow readFromLocalFolder(String path) {

    return IntegrationFlow.from(
            getFileReadingMessageSource(path),
            poller -> poller.poller(p -> p.fixedDelay(sourceConfig.fixedDelay())))
        .log(Level.WARN)
        .filter(
            source -> {
              if (source instanceof File) {
                return ((File) source).getName().endsWith(sourceConfig.fileNameFilter());
              }
              return false;
            })
        .transform(new UnZipTransformer())
        .handle(
            (payload, headers) -> {
              try {
                final File file = (File) ((TreeMap) payload).firstEntry().getValue();
                final String fileContent = new String(new FileInputStream(file).readAllBytes());
                return fileContent;
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .channel(MSG_CHANNEL_INPUT)
        .get();
  }

  private FileReadingMessageSource getFileReadingMessageSource(String path) {
    var source = new FileReadingMessageSource();
    final File directory = new File(path);
    if (!directory.isDirectory() || !directory.canRead()) {

      throw new IllegalArgumentException(
          "input path '%s' should be a readable dictionary - EXISTS: '%s' CAN_READ: '%s' IS_DICTIONARY: '%s' IS_FILE: '%s'"
              .formatted(
                  directory.toString(),
                  directory.exists(),
                  directory.canRead(),
                  directory.isDirectory(),
                  directory.isFile()));
    }
    source.setDirectory(directory);
    source.setFilter(new AcceptAllFileListFilter<>());
    source.setUseWatchService(true);
    return source;
  }

  public IntegrationFlow readFromSftp(MessageSource<InputStream> messageSource) {
    return IntegrationFlow.from(
            messageSource,
            poller ->
                poller.poller(
                    p ->
                        p.fixedDelay(sourceConfig.fixedDelay())
                            .maxMessagesPerPoll(sourceConfig.pollSize())))
        .handle(
            (payload, headers) -> {
              final Object fileRemoteFile = headers.get("file_remoteFile");
              if (fileRemoteFile == null)
                log.warn("file_remoteFile header is missing! skipping file may be a problem! ");
              if (fileRemoteFile != null
                  && fileRemoteFile.toString().endsWith(sourceConfig.fileNameFilter())) {
                return payload;
              } else {
                return null;
              }
            })
        .transform(new UnZipTransformer())
        .handle(
            (payload, headers) -> {
              try {
                return new String(
                    new FileInputStream(((File) ((TreeMap) payload).firstEntry().getValue()))
                        .readAllBytes());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .channel(MSG_CHANNEL_INPUT)
        .get();
  }
}
