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

package org.eclipse.dataspaceconnector.transfer.core.edr;

import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiver;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory implementation of {@link EndpointDataReferenceReceiverRegistry}.
 */
public class EndpointDataReferenceReceiverRegistryImpl implements EndpointDataReferenceReceiverRegistry {

    private final List<EndpointDataReferenceReceiver> receivers = new ArrayList<>();

    @Override
    public void addReceiver(@NotNull EndpointDataReferenceReceiver receiver) {
        receivers.add(receiver);
    }

    @Override
    public @NotNull List<EndpointDataReferenceReceiver> getAll() {
        return new ArrayList<>(receivers);
    }
}
