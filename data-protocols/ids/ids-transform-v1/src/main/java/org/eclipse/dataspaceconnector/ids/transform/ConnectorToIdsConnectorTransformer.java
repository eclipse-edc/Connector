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
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.BaseConnectorBuilder;
import de.fraunhofer.iais.eis.ConnectorEndpointBuilder;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.SecurityProfile;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import de.fraunhofer.iais.eis.util.Util;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.types.Connector;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class ConnectorToIdsConnectorTransformer implements IdsTypeTransformer<Connector, de.fraunhofer.iais.eis.Connector> {

    @Override
    public Class<Connector> getInputType() {
        return Connector.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.Connector> getOutputType() {
        return de.fraunhofer.iais.eis.Connector.class;
    }

    @Nullable
    @Override
    public de.fraunhofer.iais.eis.Connector transform(Connector object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        BaseConnectorBuilder builder;
        String connectorId = object.getId();
        if (connectorId != null) {
            URI connectorIdUri = URI.create(String.join(
                    IdsIdParser.DELIMITER,
                    IdsIdParser.SCHEME,
                    IdsType.CONNECTOR.getValue(),
                    connectorId));
            builder = new BaseConnectorBuilder(connectorIdUri);
        } else {
            builder = new BaseConnectorBuilder();
        }

        if (object.getDataCatalogs() != null) {
            ArrayList<ResourceCatalog> catalogs = new ArrayList<>();
            for (Catalog dataCatalog : object.getDataCatalogs()) {
                ResourceCatalog catalog = context.transform(dataCatalog, ResourceCatalog.class);
                if (catalog != null) {
                    catalogs.add(catalog);
                }
            }
            builder._resourceCatalog_(catalogs);
        }

        builder._inboundModelVersion_(new ArrayList<>(Collections.singletonList(IdsProtocol.INFORMATION_MODEL_VERSION)));
        builder._outboundModelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);

        SecurityProfile securityProfile = context.transform(object.getSecurityProfile(), SecurityProfile.class);
        if (securityProfile != null) {
            builder._securityProfile_(securityProfile);
        }

        URI uriEndpoint = object.getEndpoint();
        if (uriEndpoint != null) {
            builder._hasDefaultEndpoint_(new ConnectorEndpointBuilder()._accessURL_(uriEndpoint).build());
        }

        String dataSpaceConnectorVersion = object.getConnectorVersion();
        if (dataSpaceConnectorVersion != null) {
            builder._version_(dataSpaceConnectorVersion);
        }

        URI maintainer = object.getMaintainer();
        if (maintainer != null) {
            builder._maintainer_(maintainer);
        }

        URI curator = object.getCurator();
        if (curator != null) {
            builder._curator_(curator);
        }

        String title = object.getTitle();
        if (title != null) {
            builder._title_(Util.asList(new TypedLiteral(title)));
        }

        String description = object.getDescription();
        if (description != null) {
            builder._description_(Util.asList(new TypedLiteral(description)));
        }

        return builder.build();
    }
}
