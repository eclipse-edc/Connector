/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

/**
 * Registers {@link StreamPublisher} to receive streaming transfer requests.
 */
public interface StreamPublisherRegistry {

    /**
     * Registers the publisher.
     */
    void register(StreamPublisher publisher);

    /**
     * Notifies a {@link StreamPublisher} that can handle the request it can begin publishing data to the requested endpoint.
     */
    void notifyPublisher(DataRequest data);
}
