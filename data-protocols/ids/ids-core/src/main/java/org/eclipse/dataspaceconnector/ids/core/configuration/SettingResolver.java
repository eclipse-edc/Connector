package org.eclipse.dataspaceconnector.ids.core.configuration;

import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class SettingResolver {

    private static final String WARNING_MISSING_CONFIGURATION = "IDS Settings: No setting found for key '%s'. Using default value '%s'";
    private static final String REASON_ILLEGAL_ID = "IDS Settings: Connector ID must be an valid URN (e.g. %s )";
    private static final String REASON_ILLEGAL_URI = "IDS Settings: Expected valid URI for setting '%s', but was %s'.";
    private static final String REASON_ILLEGAL_SECURITY_PROFILE = "IDS Settings: Invalid security profile '%s'. Valid profiles: %s'.";

    private final ServiceExtensionContext serviceExtensionContext;
    private final Monitor monitor;

    public SettingResolver(@NotNull ServiceExtensionContext serviceExtensionContext) {
        this.serviceExtensionContext = Objects.requireNonNull(serviceExtensionContext);
        this.monitor = serviceExtensionContext.getMonitor();
    }

    @NotNull
    public URI resolveId() throws IllegalSettingException {
        var id = resolveUriAndWarnIfDefaultUsed(SettingKeys.EDC_IDS_ID, SettingDefaults.ID);

        try {
            var isConnectorId = IdsId.fromUri(id).getType() == IdsId.Type.CONNECTOR;
            if (!isConnectorId) {
                throw new IllegalSettingException(String.format(REASON_ILLEGAL_ID, SettingDefaults.ID));
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalSettingException(String.format(REASON_ILLEGAL_ID, SettingDefaults.ID), e);
        }

        return id;
    }

    @NotNull
    public SecurityProfile resolveSecurityProfile() throws IllegalSettingException {
        var profile = resolveTextAndWarnIfDefaultUsed(SettingKeys.EDC_IDS_SECURITY_PROFILE, SettingDefaults.SECURITY_PROFILE);

        for (SecurityProfile securityProfile : SecurityProfile.values()) {
            if (securityProfile.name().equalsIgnoreCase(profile)) {
                return securityProfile;
            }
        }

        var allowedProfiles = Arrays.stream(SecurityProfile.values()).map(Enum::toString).collect(Collectors.joining(", "));
        var reason = String.format(REASON_ILLEGAL_SECURITY_PROFILE, profile, allowedProfiles);
        throw new IllegalSettingException(reason);
    }

    @NotNull
    public String resolveTitle() {
        return resolveTextAndWarnIfDefaultUsed(SettingKeys.EDC_IDS_TITLE, SettingDefaults.TITLE);
    }

    @NotNull
    public String resolveDescription() {
        return resolveTextAndWarnIfDefaultUsed(SettingKeys.EDC_IDS_DESCRIPTION, SettingDefaults.DESCRIPTION);
    }

    @NotNull
    public URI resolveMaintainer() throws IllegalSettingException {
        return resolveUriAndWarnIfDefaultUsed(SettingKeys.EDC_IDS_MAINTAINER, SettingDefaults.MAINTAINER);
    }

    @NotNull
    public URI resolveCurator() throws IllegalSettingException {
        return resolveUriAndWarnIfDefaultUsed(SettingKeys.EDC_IDS_CURATOR, SettingDefaults.CURATOR);
    }

    @NotNull
    public URI resolveEndpoint() throws IllegalSettingException {
        return resolveUriAndWarnIfDefaultUsed(SettingKeys.EDC_IDS_ENDPOINT, SettingDefaults.ENDPOINT);
    }

    private URI resolveUriAndWarnIfDefaultUsed(String settingKey, String settingDefault) throws IllegalSettingException {
        var setting = resolveTextAndWarnIfDefaultUsed(settingKey, settingDefault);
        try {
            return new URI(setting);
        } catch (URISyntaxException e) {
            var exceptionMessage = String.format(REASON_ILLEGAL_URI, settingKey, setting);
            throw new IllegalSettingException(exceptionMessage, e);
        }
    }

    private String resolveTextAndWarnIfDefaultUsed(String settingKey, String settingDefault) {
        var setting = serviceExtensionContext.getSetting(settingKey, null);
        if (setting == null) {
            monitor.warning(String.format(WARNING_MISSING_CONFIGURATION, settingKey, settingDefault));
            setting = settingDefault;
        }
        return setting;
    }
}