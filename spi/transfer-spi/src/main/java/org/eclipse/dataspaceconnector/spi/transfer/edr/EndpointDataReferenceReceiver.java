/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.transfer.edr;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Defines what to do when an {@link EndpointDataReference} is received by the consumer Control Plane (consecutive to the Artifact Request).
 */
@FunctionalInterface
public interface EndpointDataReferenceReceiver {

    /**
     * Tells what to do when an {@link EndpointDataReference} is received, e.g. storing it into a database, send it to a http endpoint...
     */
    CompletableFuture<Result<Void>> send(@NotNull EndpointDataReference edr);
}
