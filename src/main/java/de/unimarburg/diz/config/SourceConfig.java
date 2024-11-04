/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.source")
public record SourceConfig(
    SftpSourceConfig sftp,
    String use,
    String localPath,
    String fileNameFilter,
    String propertyAsId,
    Integer pollSize,
    Integer fixedDelay) {}
