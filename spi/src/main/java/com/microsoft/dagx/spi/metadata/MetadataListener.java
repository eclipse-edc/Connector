/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.spi.metadata;

public interface MetadataListener {
    void querySubmitted();

    void searchInitiated();

    void metadataItemAdded();

    void metadataItemUpdated();
}
