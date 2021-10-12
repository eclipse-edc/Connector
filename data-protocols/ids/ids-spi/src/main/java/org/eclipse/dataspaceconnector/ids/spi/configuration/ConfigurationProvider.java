package org.eclipse.dataspaceconnector.ids.spi.configuration;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

public interface ConfigurationProvider {

    Path resolveCertificatePath();

    Path resolveKeyPairPath();

    Optional<String> resolveKeyPairPassphrase();

    URL resolveDynamicAttributeProvisionServiceTokenUrl();
}
