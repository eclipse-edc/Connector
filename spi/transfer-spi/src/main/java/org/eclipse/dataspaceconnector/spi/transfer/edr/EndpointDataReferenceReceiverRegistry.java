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

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registry for {@link EndpointDataReferenceReceiver}.
 */
public interface EndpointDataReferenceReceiverRegistry {

    void addReceiver(@NotNull EndpointDataReferenceReceiver receiver);

    @NotNull
    List<EndpointDataReferenceReceiver> getAll();
}
