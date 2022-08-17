/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.edr;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory implementation of {@link EndpointDataReferenceTransformerRegistry}.
 */
public class EndpointDataReferenceTransformerRegistryImpl implements EndpointDataReferenceTransformerRegistry {

    private final List<EndpointDataReferenceTransformer> transformers = new ArrayList<>();

    @Override
    public void registerTransformer(@NotNull EndpointDataReferenceTransformer transformer) {
        transformers.add(transformer);
    }

    /**
     * Applies first matching {@link EndpointDataReferenceTransformer}. If none can handle the provided {@link EndpointDataReference}
     * then it is returned as such.
     */
    @Override
    public @NotNull Result<EndpointDataReference> transform(@NotNull EndpointDataReference edr) {
        return transformers.stream()
                .filter(t -> t.canHandle(edr))
                .findFirst()
                .map(t -> t.transform(edr))
                .orElse(Result.success(edr));
    }
}
