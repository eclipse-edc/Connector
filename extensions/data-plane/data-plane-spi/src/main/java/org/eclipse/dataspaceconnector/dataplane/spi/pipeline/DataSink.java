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
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.dataplane.spi.pipeline;

import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.concurrent.CompletableFuture;

/**
 * A data sink.
 */
public interface DataSink {

    /**
     * Transfers the data to the sink, returning a future to obtain the result. Implementations may be non-blocking.
     */
    CompletableFuture<Result<Void>> transfer(DataSource source);
}
