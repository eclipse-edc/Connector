/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.azure.blob.core.adapter.BlobAdapter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

class FakeBlobAdapter implements BlobAdapter {
    static Faker faker = new Faker();

    final String name = faker.lorem().characters();
    final String content = faker.lorem().sentence();
    final long length = faker.random().nextLong(1_000_000_000_000_000L);
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @Override
    public OutputStream getOutputStream() {
        return out;
    }

    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream(content.getBytes(UTF_8));
    }

    @Override
    public String getBlobName() {
        return name;
    }

    @Override
    public long getBlobSize() {
        return length;
    }
}

