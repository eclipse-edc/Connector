/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.edc.spi.metadata;

public interface MetadataListener {
    void querySubmitted();

    void searchInitiated();

    void metadataItemAdded();

    void metadataItemUpdated();
}
