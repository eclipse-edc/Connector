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
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapter;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AzureStorageTestFixtures {

    static Faker faker = new Faker();

    static DataFlowRequest.Builder createRequest(String type) {
        return DataFlowRequest.Builder.newInstance()
                .id("1").processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type(type).build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type(type).build());
    }

    public static String createAccountName() {
        return faker.lorem().characters(3, 24, false, false);
    }

    public static String createContainerName() {
        return faker.lorem().characters(3, 40, false, false);
    }

    public static String createBlobName() {
        return faker.lorem().characters(3, 40, false, false);
    }

    public static String createSharedKey() {
        return faker.lorem().characters();
    }

    private AzureStorageTestFixtures() {
    }

    static class FakeBlobAdapter implements BlobAdapter {
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

    static class TestCustomException extends RuntimeException {
        TestCustomException(String message) {
            super(message);
        }
    }
}
