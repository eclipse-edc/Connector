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
 *       Cofinity-X - unauthenticated DSP version endpoint
 *
 */

package org.eclipse.edc.connector.controlplane.services.spi.protocol;

import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Mediates access of {@link ProtocolVersion} on protocol communication.
 */
public interface VersionProtocolService {

    /**
     * Get all the versions.
     *
     * @return a {@link ServiceResult} with the list of versions.
     */
    ServiceResult<ProtocolVersions> getAll();
}
