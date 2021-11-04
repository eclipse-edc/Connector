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

import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

public class UriToIdsTypeTransformer implements IdsTypeTransformer<URI, IdsType> {

    @Override
    public Class<URI> getInputType() {
        return URI.class;
    }

    @Override
    public Class<IdsType> getOutputType() {
        return IdsType.class;
    }

    @Override
    public @Nullable IdsType transform(URI object, TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        var id = context.transform(object, IdsId.class);
        if (id == null) {
            context.reportProblem("URI cannot be mapped to IdsId");
            return null;
        }

        return id.getType();
    }
}
