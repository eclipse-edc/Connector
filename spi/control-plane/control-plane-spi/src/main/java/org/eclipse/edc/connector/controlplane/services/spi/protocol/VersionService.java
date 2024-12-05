/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services.spi.protocol;

import org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.concurrent.CompletableFuture;

/**
 * Service to request versions from a counter-party.
 */
public interface VersionService {

    /**
     * Request versions from the counter-party.
     *
     * @param request the protocol version request {@link ProtocolVersionRequest}.
     * @return the supported versions.
     */
    CompletableFuture<StatusResult<byte[]>> requestVersions(ProtocolVersionRequest request);

}
