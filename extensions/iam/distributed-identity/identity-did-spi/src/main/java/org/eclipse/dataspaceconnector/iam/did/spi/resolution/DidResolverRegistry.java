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
package org.eclipse.dataspaceconnector.iam.did.spi.resolution;

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;

/**
 * Delegates to a {@link DidResolver} to resolve a DID document.
 */
public interface DidResolverRegistry {
    String FEATURE = "edc:identity:did:resolver-registry";

    /**
     * Registers a DID resolver.
     */
    void register(DidResolver resolver);

    /**
     * Resolves a DID document based on the DID method.
     */
    Result resolve(String didKey);

    /**
     * The response to a resolution operation.
     */
    class Result {
        private DidDocument didDocument;
        private String invalidMessage;

        public Result(DidDocument didDocument) {
            this.didDocument = didDocument;
        }

        public Result(String invalidMessage) {
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
}
