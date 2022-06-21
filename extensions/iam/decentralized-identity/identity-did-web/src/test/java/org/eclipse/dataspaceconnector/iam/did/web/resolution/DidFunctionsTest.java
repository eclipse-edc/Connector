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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.eclipse.dataspaceconnector.iam.did.web.resolution.DidFunctions.keyToUrl;

/**
 * Verifies DID functions.
 */
class DidFunctionsTest {

    /**
     * Verifies Web DID URN mappings.
     */
    @Test
    void verifyConvertToUrl() throws Exception {
        Assertions.assertThat(keyToUrl("did:web:w3c-ccg.github.io")).isEqualTo("https://w3c-ccg.github.io/.well-known/did.json");
        Assertions.assertThat(keyToUrl("did:web:w3c-ccg.github.io:user:alice")).isEqualTo("https://w3c-ccg.github.io/user/alice/did.json");

        assertThatIllegalArgumentException().isThrownBy(() -> keyToUrl("did:web:w3c-ccg.github.io:user:alice:"));
    }


}
