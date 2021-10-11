package org.eclipse.dataspaceconnector.ids.core.configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.eclipse.dataspaceconnector.ids.spi.configuration.ConfigurationProvider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class ConfigurationProviderImpl implements ConfigurationProvider {

    private final ServiceExtensionContext serviceExtensionContext;

    public ConfigurationProviderImpl(final ServiceExtensionContext serviceExtensionContext) {
        this.serviceExtensionContext = serviceExtensionContext;
    }

    @Override
    public Path resolveCertificatePath() {
        final String setting = resolveRequiredSetting(IdsSettings.EDC_IDS_DAPS_CERTIFICATE_PATH);
        return Paths.get(setting);
    }

    @Override
    public Path resolveKeyPairPath() {
        final String setting = resolveRequiredSetting(IdsSettings.EDC_IDS_DAPS_PRIVATEKEY_PATH);
        return Paths.get(setting);
    }

    @Override
    public Optional<String> resolveKeyPairPassphrase() {
        return Optional.ofNullable(serviceExtensionContext.getSetting(IdsSettings.EDC_IDS_DAPS_PRIVATEKEY_PASSPHRASE, null));
    }

    @Override
    public URL resolveDynamicAttributeProvisionServiceTokenUrl() {
        final String setting = resolveRequiredSetting(IdsSettings.EDC_IDS_DAPS_TOKEN_URL);
        try {
            return new URL(setting);
        } catch (MalformedURLException e) {
            throw new InvalidConfigurationException(String.format("invalid configuration: %s", IdsSettings.EDC_IDS_DAPS_TOKEN_URL), e);
        }
    }

    private String resolveRequiredSetting(final String key) {
        String setting = serviceExtensionContext.getSetting(key, null);
        if (setting == null) {
            throw new MissingConfigurationException(String.format("mandatory configuration missing: %s", key));
        }
        return setting;
    }
}
