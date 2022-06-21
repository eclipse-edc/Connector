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
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

public class UriToIdsIdTransformer implements IdsTypeTransformer<URI, IdsId> {

    @Override
    public Class<URI> getInputType() {
        return URI.class;
    }

    @Override
    public Class<IdsId> getOutputType() {
        return IdsId.class;
    }

    @Override
    public @Nullable IdsId transform(URI object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        try {
            return IdsIdParser.parse(object.getScheme() + IdsIdParser.DELIMITER + object.getSchemeSpecificPart());
        } catch (IllegalArgumentException e) {
            context.reportProblem(String.format("Could not transform URI to IdsId: %s", e.getMessage()));
        }

        return null;
    }
}
