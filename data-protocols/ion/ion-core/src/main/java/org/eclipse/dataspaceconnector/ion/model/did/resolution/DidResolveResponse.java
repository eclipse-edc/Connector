package org.eclipse.dataspaceconnector.ion.model.did.resolution;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO representation of the JSON response that ION returns after a resolve request.
 * Consists of a context string, a {@link DidDocument} object and a {@link DidDocumentMetadata} object.
 */
public class DidResolveResponse {
    String context;
    DidDocument didDocument;
    DidDocumentMetadata didDocumentMetadata;

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
