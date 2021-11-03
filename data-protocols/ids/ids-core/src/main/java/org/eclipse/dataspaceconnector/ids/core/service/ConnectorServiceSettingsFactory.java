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

package org.eclipse.dataspaceconnector.ids.core.service;

import org.eclipse.dataspaceconnector.ids.core.configuration.IllegalSettingException;
import org.eclipse.dataspaceconnector.ids.core.configuration.SettingResolver;
import org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConnectorServiceSettingsFactory {
    private final SettingResolver settingResolver;

    public ConnectorServiceSettingsFactory(@NotNull SettingResolver settingResolver) {
        this.settingResolver = Objects.requireNonNull(settingResolver);
    }

    @NotNull
    public ConnectorServiceSettingsFactoryResult getSettingsResult() {
        List<String> errors = new ArrayList<>();

        String title;
        String description;
        String id = null;
        URI maintainer = null;
        URI curator = null;
        URI connectorEndpoint = null;
        SecurityProfile securityProfile = null;

        try {
            id = settingResolver.resolveId();
        } catch (IllegalSettingException e) {
            errors.add(e.getMessage());
        }

        title = settingResolver.resolveTitle();
        description = settingResolver.resolveDescription();

        try {
            maintainer = settingResolver.resolveMaintainer();
        } catch (IllegalSettingException e) {
            errors.add(e.getMessage());
        }

        try {
            curator = settingResolver.resolveCurator();
        } catch (IllegalSettingException e) {
            errors.add(e.getMessage());
        }

        try {
            connectorEndpoint = settingResolver.resolveEndpoint();
        } catch (IllegalSettingException e) {
            errors.add(e.getMessage());
        }

        try {
            securityProfile = settingResolver.resolveSecurityProfile();
        } catch (IllegalSettingException e) {
            errors.add(e.getMessage());
        }

        var settings = ConnectorServiceSettings.Builder.newInstance()
                .id(id)
                .title(title)
                .description(description)
                .maintainer(maintainer)
                .curator(curator)
                .endpoint(connectorEndpoint)
                .securityProfile(securityProfile)
                .build();

        return ConnectorServiceSettingsFactoryResult.Builder.newInstance()
                .settings(settings)
                .errors(errors)
                .build();
    }
}
