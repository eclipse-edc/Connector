/*
 *  Copyright (c) 2023 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH - initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.pipeline;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;

/**
 * A {@link DataSource} backed by a byte array message which produces multiple instances
 */
public class MultipleBinaryPartsDataSource implements DataSource {
    private final String name;
    private final byte[] message;
    private final int messageCount;

    public MultipleBinaryPartsDataSource(String name, byte[] message, int messageCount) {
        this.name = name;
        this.message = message;
        this.messageCount = messageCount;
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        Stream.Builder<Part> builder = Stream.builder();
        for (int count = 0; count < messageCount; count++) {
            builder.add(new Part() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public InputStream openStream() {
                    return new ByteArrayInputStream(message);
                }

                @Override
                public long size() {
                    return message.length;
                }
            });
        }
        return success(builder.build());
    }

    @Override
    public void close() {
    }

}
