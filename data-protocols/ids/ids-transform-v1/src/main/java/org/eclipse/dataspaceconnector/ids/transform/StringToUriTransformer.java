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

import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class StringToUriTransformer implements IdsTypeTransformer<String, URI> {
    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public Class<URI> getOutputType() {
        return URI.class;
    }

    @Override
    public @Nullable URI transform(String object, TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        try {
            return new URI(object);
        } catch (URISyntaxException e) {
            context.reportProblem(String.format("Error transforming String to URI: %s", e.getMessage()));
        }

        return null;
    }
}
