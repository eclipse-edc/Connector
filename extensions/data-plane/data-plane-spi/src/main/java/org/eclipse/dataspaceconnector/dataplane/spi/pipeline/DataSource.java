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

import java.io.InputStream;
import java.util.stream.Stream;

/**
 * Implements pull semantics for accessing a data source. A data source is composed of one or more named parts. Some implementations may support random access of the underlying
 * part content so that large content transfers can be parallelized.
 */
public interface DataSource {

    /**
     * Opens a stream to the source parts.
     */
    Stream<Part> openStream();

    /**
     * A data source part. This is typically an underlying file or container that the data contains.
     */
    interface Part extends AutoCloseable {

        /**
         * The part name.
         */
        String name();

        /**
         * The size of the part, or -1 if the size cannot be determined.
         */
        long size();

        /**
         * Opens stream to sequentially read the underlying part content.
         */
        InputStream openStream();

        /**
         * Returns true if the part supports random access of its contents. If random access is supported, {@link #read(long, long)} may be invoked.
         */
        default boolean supportsRandomAccess() {
            return false;
        }

        /**
         * Reads a segment of the underlying data. Implementations must throw {@link UnsupportedOperationException} if random access is not supported.
         */
        default byte[] read(long offset, long bytes) {
            throw new UnsupportedOperationException("Random access not supported");
        }

        default void close() throws Exception {
            // no-op
        }
    }

}
