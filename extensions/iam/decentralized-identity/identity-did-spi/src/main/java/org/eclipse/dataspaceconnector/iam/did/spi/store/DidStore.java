/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.spi.store;

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Persists {@link DidDocument}s.
 */
public interface DidStore {

    /**
     * Returns all stored documents up to the limit.
     */
    List<DidDocument> getAll(int limit);

    /**
     * Returns all documents starting from the position specified by the given token.
     */
    List<DidDocument> getAfter(String continuationToken);

    /**
     * Persists a document.
     */
    boolean save(DidDocument document);

    /**
     * Persists a collection of documents.
     */
    void saveAll(Collection<DidDocument> documents);

    /**
     * Returns the last persisted document of null if none are found.
     */
    @Nullable
    DidDocument getLatest();

    /**
     * Returns the document corresponding to the DID or null if not found.
     */
    @Nullable
    DidDocument forId(String did);
}
