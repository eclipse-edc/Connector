/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.core.configuration;

import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.configuration.SettingKeys;
import org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SettingResolver {
    private static final Map<String, Set<String>> DEFAULT_WARNINGS = new HashMap<>();
    private static final String WARNING_MISSING_CONFIGURATION = "IDS Settings: No setting found for key '%s'. Using default value '%s'";
    private static final String REASON_ILLEGAL_URI = "IDS Settings: Expected valid URI for setting '%s', but was %s'.";

    private final ServiceExtensionContext serviceExtensionContext;
    private final Monitor monitor;

    public SettingResolver(@NotNull ServiceExtensionContext serviceExtensionContext) {
        this.serviceExtensionContext = Objects.requireNonNull(serviceExtensionContext);
        this.monitor = serviceExtensionContext.getMonitor();
    }

    private String getSetting(@NotNull String settingKey) {
        Objects.requireNonNull(settingKey);

        return serviceExtensionContext.getSetting(settingKey, null);
    }

    private String getSetting(@NotNull String settingKey, String defaultValueFunction) {
        Objects.requireNonNull(settingKey);

        String value = getSetting(settingKey);

        if (value == null) {
            reportIfDefaultUsed(settingKey, defaultValueFunction);

            value = defaultValueFunction;
        }

        return value;
    }

    private void reportIfDefaultUsed(String settingKey, String settingValue) {
        if (settingValue == null) {
            return;
        }

        if (DEFAULT_WARNINGS.computeIfAbsent(settingKey, (k) -> new HashSet<>()).add(settingValue)) {
            monitor.warning(String.format(WARNING_MISSING_CONFIGURATION, settingKey, settingValue));
        }
    }

    @NotNull
    public String resolveCatalogId() throws IllegalSettingException {
        String configuredCatalogId = getSetting(SettingKeys.EDC_IDS_CATALOG_ID, SettingDefaults.EDC_IDS_CATALOG_ID);

        try {
            URI uri = URI.create(configuredCatalogId);
            // Hint: use stringified uri to keep uri path and query
            IdsId idsId = IdsIdParser.parse(uri.toString());
            if (idsId != null && idsId.getType() == IdsType.CATALOG) {
                return idsId.getValue();
            }
        } catch (IllegalArgumentException e) {
            return configuredCatalogId;
        }

        return configuredCatalogId;
    }

    @NotNull
    public String resolveId() throws IllegalSettingException {
        String configuredConnectorId = getSetting(SettingKeys.EDC_IDS_ID, SettingDefaults.EDC_IDS_ID);

        try {
            URI uri = URI.create(configuredConnectorId);
            // Hint: use stringified uri to keep uri path and query
            IdsId idsId = IdsIdParser.parse(uri.toString());
            if (idsId != null && idsId.getType() == IdsType.CONNECTOR) {
                return idsId.getValue();
            }
        } catch (IllegalArgumentException e) {
            return configuredConnectorId;
        }

        return configuredConnectorId;
    }

    @NotNull
    public SecurityProfile resolveSecurityProfile() throws IllegalSettingException {
        String configuredSecurityProfile = getSetting(SettingKeys.EDC_IDS_SECURITY_PROFILE, SettingDefaults.SECURITY_PROFILE);

        try {
            return SecurityProfile.fromValue(configuredSecurityProfile);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalSettingException(
                    SettingKeys.EDC_IDS_SECURITY_PROFILE,
                    illegalArgumentException.getMessage()
            );
        }
    }

    @NotNull
    public String resolveTitle() {
        return getSetting(SettingKeys.EDC_IDS_TITLE, SettingDefaults.EDC_IDS_TITLE);
    }

    @NotNull
    public String resolveDescription() {
        return getSetting(SettingKeys.EDC_IDS_DESCRIPTION, SettingDefaults.EDC_IDS_DESCRIPTION);
    }

    @NotNull
    public URI resolveMaintainer() throws IllegalSettingException {
        String configuredMaintainer = getSetting(SettingKeys.EDC_IDS_MAINTAINER, SettingDefaults.EDC_IDS_MAINTAINER);

        try {
            return new URI(configuredMaintainer);
        } catch (URISyntaxException e) {
            throw new IllegalSettingException(
                    SettingKeys.EDC_IDS_MAINTAINER,
                    String.format(
                            REASON_ILLEGAL_URI,
                            SettingKeys.EDC_IDS_MAINTAINER,
                            configuredMaintainer
                    ), e);
        }
    }

    @NotNull
    public URI resolveCurator() throws IllegalSettingException {
        String configuredCurator = getSetting(SettingKeys.EDC_IDS_CURATOR, SettingDefaults.EDC_IDS_CURATOR);

        try {
            return new URI(configuredCurator);
        } catch (URISyntaxException e) {
            throw new IllegalSettingException(
                    SettingKeys.EDC_IDS_CURATOR,
                    String.format(
                            REASON_ILLEGAL_URI,
                            SettingKeys.EDC_IDS_CURATOR,
                            configuredCurator
                    ), e);
        }
    }

    @NotNull
    public URI resolveEndpoint() throws IllegalSettingException {
        String configuredEndpoint = getSetting(SettingKeys.EDC_IDS_ENDPOINT, SettingDefaults.EDC_IDS_ENDPOINT);

        try {
            return new URI(configuredEndpoint);
        } catch (URISyntaxException e) {
            throw new IllegalSettingException(
                    SettingKeys.EDC_IDS_CURATOR,
                    String.format(
                            REASON_ILLEGAL_URI,
                            SettingKeys.EDC_IDS_CURATOR,
                            configuredEndpoint
                    ), e);
        }
    }
}