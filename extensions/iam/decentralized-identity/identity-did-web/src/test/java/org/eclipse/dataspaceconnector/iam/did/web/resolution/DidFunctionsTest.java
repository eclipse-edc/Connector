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

package org.eclipse.dataspaceconnector.iam.did.web.resolution;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.eclipse.dataspaceconnector.iam.did.web.resolution.DidFunctions.resolveDidDocumentUrl;

/**
 * Verifies DID functions.
 */
class DidFunctionsTest {

    /**
     * Verifies Web DID URN mappings.
     */
    @Test
    void verifyResolveDidDocumentUrl() {
        assertThat(resolveDidDocumentUrl("did:web:w3c-ccg.github.io", true)).isEqualTo("https://w3c-ccg.github.io/.well-known/did.json");
        assertThat(resolveDidDocumentUrl("did:web:w3c-ccg.github.io", false)).isEqualTo("http://w3c-ccg.github.io/.well-known/did.json");
        assertThat(resolveDidDocumentUrl("did:web:w3c-ccg.github.io:user:alice", true)).isEqualTo("https://w3c-ccg.github.io/user/alice/did.json");

        assertThatIllegalArgumentException().isThrownBy(() -> resolveDidDocumentUrl("did:web:w3c-ccg.github.io:user:alice:", true));
    }


}
