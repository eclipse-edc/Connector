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

package org.eclipse.dataspaceconnector.ids.api.multipart.factory;

import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.BaseConnectorBuilder;
import de.fraunhofer.iais.eis.ConnectorEndpoint;
import de.fraunhofer.iais.eis.ConnectorEndpointBuilder;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.SecurityProfile;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import de.fraunhofer.iais.eis.util.Util;
import org.eclipse.dataspaceconnector.ids.spi.version.ConnectorVersionProvider;
import org.eclipse.dataspaceconnector.ids.spi.version.IdsProtocol;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

@Deprecated // This functionality will be moved to a transformer class
public class BaseConnectorFactory {

    private final BaseConnectorFactorySettings baseConnectorFactorySettings;
    private final ConnectorVersionProvider connectorVersionProvider;

    public BaseConnectorFactory(
            @NotNull BaseConnectorFactorySettings baseConnectorFactorySettings,
            @NotNull ConnectorVersionProvider connectorVersionProvider) {
        this.baseConnectorFactorySettings = Objects.requireNonNull(baseConnectorFactorySettings);
        this.connectorVersionProvider = Objects.requireNonNull(connectorVersionProvider);
    }

    public BaseConnector createBaseConnector(
            ResourceCatalog... resourceCatalog) {

        URI connectorId = baseConnectorFactorySettings.getId();
        BaseConnectorBuilder builder;
        if (connectorId != null) {
            builder = new BaseConnectorBuilder(connectorId);
        } else {
            builder = new BaseConnectorBuilder();
        }

        builder._resourceCatalog_(new ArrayList<>(Arrays.asList(resourceCatalog)));
        builder._inboundModelVersion_(new ArrayList<>(Collections.singletonList(IdsProtocol.INFORMATION_MODEL_VERSION)));

        // TODO There should be a process how the security profile is defined/found
        SecurityProfile securityProfile = resolveSecurityProfile();
        if (securityProfile != null) {
            builder._securityProfile_(securityProfile);
        }

        ConnectorEndpoint connectorEndpoint = resolveConnectorEndpoint();
        if (connectorEndpoint != null) {
            builder._hasDefaultEndpoint_(connectorEndpoint);
        }

        String dataSpaceConnectorVersion = connectorVersionProvider.getVersion();
        if (dataSpaceConnectorVersion != null) {
            builder._version_(dataSpaceConnectorVersion);
        }

        URI maintainer = baseConnectorFactorySettings.getMaintainer();
        if (maintainer != null) {
            builder._maintainer_(maintainer);
        }

        URI curator = baseConnectorFactorySettings.getCurator();
        if (curator != null) {
            builder._curator_(curator);
        }

        String title = baseConnectorFactorySettings.getTitle();
        if (title != null) {
            builder._title_(Util.asList(new TypedLiteral(title)));
        }

        String description = baseConnectorFactorySettings.getDescription();
        if (description != null) {
            builder._description_(Util.asList(new TypedLiteral(description)));
        }

        return builder.build();
    }

    private SecurityProfile resolveSecurityProfile() {
        org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile configuredProfile = baseConnectorFactorySettings.getSecurityProfile();

        if (configuredProfile == null) {
            return null;
        }

        for (SecurityProfile securityProfile : SecurityProfile.values()) {
            if (securityProfile.name().equalsIgnoreCase(configuredProfile.name())) {
                return securityProfile;
            }
        }

        return null;
    }


    private ConnectorEndpoint resolveConnectorEndpoint() {
        URI endpointUri = baseConnectorFactorySettings.getConnectorEndpoint();
        ConnectorEndpoint connectorEndpoint = null;
        if (endpointUri != null) {
            connectorEndpoint = createConnectorEndpoint(endpointUri);
        }
        return connectorEndpoint;
    }

    private ConnectorEndpoint createConnectorEndpoint(URI uri) {
        ConnectorEndpointBuilder endpoint = new ConnectorEndpointBuilder();
        endpoint._accessURL_(uri);
        return endpoint.build();
    }
}
