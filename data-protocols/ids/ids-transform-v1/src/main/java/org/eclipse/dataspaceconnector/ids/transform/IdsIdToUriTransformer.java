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

public class IdsIdToUriTransformer implements IdsTypeTransformer<IdsId, URI> {
    @Override
    public Class<IdsId> getInputType() {
        return IdsId.class;
    }

    @Override
    public Class<URI> getOutputType() {
        return URI.class;
    }

    @Override
    public @Nullable URI transform(IdsId object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        try {
            return URI.create(String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, object.getType().getValue(), object.getValue()));
        } catch (IllegalArgumentException e) {
            context.reportProblem(String.format("Could not transform IdsId to URI: %s", e.getMessage()));
        }

        return null;
    }
}
