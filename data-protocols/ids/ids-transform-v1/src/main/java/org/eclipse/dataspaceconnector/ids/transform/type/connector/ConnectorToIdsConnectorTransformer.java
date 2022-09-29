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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.dataspaceconnector.ids.transform.type.connector;

import de.fraunhofer.iais.eis.BaseConnectorBuilder;
import de.fraunhofer.iais.eis.ConnectorEndpointBuilder;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.SecurityProfile;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import org.eclipse.dataspaceconnector.ids.spi.domain.IdsConstants;
import org.eclipse.dataspaceconnector.ids.spi.domain.connector.Connector;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        
        var builder = new BaseConnectorBuilder(object.getId().toUri());

        if (object.getDataCatalogs() != null) {
            var catalogs = new ArrayList<ResourceCatalog>();
            for (var dataCatalog : object.getDataCatalogs()) {
                var result = context.transform(dataCatalog, ResourceCatalog.class);
                if (result != null) {
                    catalogs.add(result);
                }
            }

            builder._resourceCatalog_(catalogs);
        }

        builder._inboundModelVersion_(new ArrayList<>(Collections.singletonList(IdsConstants.INFORMATION_MODEL_VERSION)));
        builder._outboundModelVersion_(IdsConstants.INFORMATION_MODEL_VERSION);

        var securityProfile = context.transform(object.getSecurityProfile(), SecurityProfile.class);
        if (securityProfile != null) {
            builder._securityProfile_(securityProfile);
        }

        var uriEndpoint = object.getEndpoint();
        if (uriEndpoint != null) {
            builder._hasDefaultEndpoint_(new ConnectorEndpointBuilder()._accessURL_(uriEndpoint).build());
        }

        var connectorVersion = object.getConnectorVersion();
        if (connectorVersion != null) {
            builder._version_(connectorVersion);
        }

        var maintainer = object.getMaintainer();
        if (maintainer != null) {
            builder._maintainer_(maintainer);
        }

        var curator = object.getCurator();
        if (curator != null) {
            builder._curator_(curator);
        }

        var title = object.getTitle();
        if (title != null) {
            builder._title_(List.of(new TypedLiteral(title)));
        }

        var description = object.getDescription();
        if (description != null) {
            builder._description_(List.of(new TypedLiteral(description)));
        }

        return builder.build();
    }
}
