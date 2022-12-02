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

package org.eclipse.edc.connector.transfer.dataplane.spi.proxy;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

/**
 * Service that generated an {@link EndpointDataReference} for a http data proxy.
 */
@FunctionalInterface
public interface ConsumerPullTransferEndpointDataReferenceService {
    Result<EndpointDataReference> createProxyReference(@NotNull ConsumerPullTransferEndpointDataReferenceCreationRequest request);
}
