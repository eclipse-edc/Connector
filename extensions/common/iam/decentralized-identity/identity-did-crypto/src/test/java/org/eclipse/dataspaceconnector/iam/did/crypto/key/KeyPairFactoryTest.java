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
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.crypto.key;

import org.junit.jupiter.api.Test;

class KeyPairFactoryTest {

    @Test
    void generateKeyPairP256() {
        // assert no exception is thrown
        KeyPairFactory.generateKeyPairP256();
    }
}
