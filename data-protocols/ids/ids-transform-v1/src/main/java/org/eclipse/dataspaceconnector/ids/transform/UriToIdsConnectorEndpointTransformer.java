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

import de.fraunhofer.iais.eis.ConnectorEndpoint;
import de.fraunhofer.iais.eis.ConnectorEndpointBuilder;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

public class UriToIdsConnectorEndpointTransformer implements IdsTypeTransformer<URI, ConnectorEndpoint> {

    @Override
    public Class<URI> getInputType() {
        return URI.class;
    }

    @Override
    public Class<ConnectorEndpoint> getOutputType() {
        return ConnectorEndpoint.class;
    }

    @Override
    public @Nullable ConnectorEndpoint transform(URI object, TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        ConnectorEndpointBuilder endpoint = new ConnectorEndpointBuilder(object);
        endpoint._accessURL_(object);

        return endpoint.build();
    }
}
