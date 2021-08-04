/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.dataspaceconnector.spi.metadata;

public interface MetadataListener {
    void querySubmitted();

    void searchInitiated();

    void metadataItemAdded();

    void metadataItemUpdated();
}
