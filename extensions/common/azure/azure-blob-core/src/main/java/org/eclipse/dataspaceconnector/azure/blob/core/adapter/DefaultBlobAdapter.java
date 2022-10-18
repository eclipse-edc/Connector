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

package org.eclipse.dataspaceconnector.azure.blob.core.adapter;

import com.azure.storage.blob.specialized.BlockBlobClient;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementation of {@link BlobAdapter} using a {@link BlockBlobClient}.
 */
public class DefaultBlobAdapter implements BlobAdapter {
    private final BlockBlobClient client;

    public DefaultBlobAdapter(BlockBlobClient client) {
        this.client = client;
    }

    @Override
    public OutputStream getOutputStream() {
        return client.getBlobOutputStream(/* overwrite = */ true);
    }

    @Override
    public InputStream openInputStream() {
        return client.openInputStream();
    }

    @Override
    public String getBlobName() {
        return client.getBlobName();
    }

    @Override
    public long getBlobSize() {
        return client.getProperties().getBlobSize();
    }
}
