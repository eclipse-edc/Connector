/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.selector.spi.client;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;

/**
 * Factory for {@link DataPlaneClient} instances.
 */
@FunctionalInterface
public interface DataPlaneClientFactory {

    /**
     * Create a {@link DataPlaneClient} that points to the {@link DataPlaneInstance}
     *
     * @param dataPlaneInstance the data plane instance.
     * @return the data plane client.
     */
    DataPlaneClient createClient(DataPlaneInstance dataPlaneInstance);

}
