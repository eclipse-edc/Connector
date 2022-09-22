package org.eclipse.dataspaceconnector.iam.oauth2.spi;

import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * No-op implementation for CredentialsRequestAdditionalParametersProvider
 */
public class NoopCredentialsRequestAdditionalParametersProvider implements CredentialsRequestAdditionalParametersProvider {

    @Override
    public @NotNull Map<String, String> provide(TokenParameters parameters) {
        return emptyMap();
    }
}
