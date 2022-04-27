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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.spi;


import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;

import java.util.concurrent.BlockingQueue;

/**
 * Manages a list of {@link Loader}s.
 * If for example a Queue is used to receive {@link org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse} objects,
 * the LoaderManager's job is to coordinate all its {@link Loader}s and forward that batch to them.
 */
public interface LoaderManager {


    /**
     * Begins observing
     */
    void start(BlockingQueue<UpdateResponse> queue);

    void stop();

    void addLoader(Loader loader);
}
