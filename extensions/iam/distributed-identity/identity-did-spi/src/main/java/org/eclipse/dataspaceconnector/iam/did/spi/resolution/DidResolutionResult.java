package org.eclipse.dataspaceconnector.iam.did.spi.resolution;

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;

/**
 * The response to a DID resolution operation.
 */
public class DidResolutionResult {
    private DidDocument didDocument;
    private String invalidMessage;

    public DidResolutionResult(DidDocument didDocument) {
        this.didDocument = didDocument;
    }

    public DidResolutionResult(String invalidMessage) {
        this.invalidMessage = invalidMessage;
    }

    public boolean invalid() {
        return invalidMessage != null;
    }

    public DidDocument getDidDocument() {
        return didDocument;
    }

    public String getInvalidMessage() {
        return invalidMessage;
    }

}
