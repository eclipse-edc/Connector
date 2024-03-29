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

package org.eclipse.edc.connector.controlplane.transfer.spi.edr;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.NotNull;

/**
 * Registry for {@link EndpointDataReferenceReceiver}.
 */
@ExtensionPoint
public interface EndpointDataReferenceReceiverRegistry {
    /**
     * Adds a new {@link EndpointDataReferenceReceiver} into the registry.
     */
    void registerReceiver(@NotNull EndpointDataReferenceReceiver receiver);
}
