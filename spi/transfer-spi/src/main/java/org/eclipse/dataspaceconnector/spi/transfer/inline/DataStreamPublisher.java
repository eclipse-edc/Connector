/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.transfer.inline;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

/**
 * A system responsible for publishing data to a stream destination.
 */
public interface DataStreamPublisher {

    /**
     * Initializes the publisher with a context for creating stream sessions.
     *
     * @param context the context
     */
    void initialize(StreamContext context);

    /**
     * Returns true if this publisher can service the request.
     */
    boolean canHandle(DataRequest dataRequest);

    /**
     * Notifies the publisher of a data request. The publisher may beging publishing data to the associated stream.
     *
     * @param dataRequest the data request
     */
    Result<Void> notifyPublisher(DataRequest dataRequest);
}