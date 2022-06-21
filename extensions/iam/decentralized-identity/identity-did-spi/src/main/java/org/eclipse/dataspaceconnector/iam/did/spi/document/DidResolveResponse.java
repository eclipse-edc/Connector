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

package org.eclipse.dataspaceconnector.iam.did.spi.document;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO representation of the JSON response that ION returns after a resolve request.
 * Consists of a context string, a {@link DidDocument} object and a {@link DidDocumentMetadata} object.
 */
public class DidResolveResponse {
    private String context;
    private DidDocument didDocument;
    private DidDocumentMetadata didDocumentMetadata;

    public DidResolveResponse() {
    }

    @JsonProperty("@context")
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    @JsonProperty("didDocument")
    public DidDocument getDidDocument() {
        return didDocument;
    }

    public void setDidDocument(DidDocument didDocument) {
        this.didDocument = didDocument;
    }

    @JsonProperty("didDocumentMetadata")
    public DidDocumentMetadata getDidDocumentMetadata() {
        return didDocumentMetadata;
    }

    public void setDidDocumentMetadata(DidDocumentMetadata didDocumentMetadata) {
        this.didDocumentMetadata = didDocumentMetadata;
    }
}
