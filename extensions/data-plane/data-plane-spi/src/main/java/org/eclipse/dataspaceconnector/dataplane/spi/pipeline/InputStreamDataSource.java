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
 * A {@link DataSource} backed by an input stream.
 */
public class InputStreamDataSource implements DataSource, DataSource.Part {
    private final String name;
    private final InputStream stream;

    public InputStreamDataSource(String name, InputStream stream) {
        this.name = name;
        this.stream = stream;
    }

    @Override
    public Stream<Part> openPartStream() {
        return Stream.of(this);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public long size() {
        return -1;
    }

    @Override
    public InputStream openStream() {
        return stream;
    }
}
