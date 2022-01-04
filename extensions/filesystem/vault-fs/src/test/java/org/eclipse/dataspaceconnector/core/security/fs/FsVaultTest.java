/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.core.security.fs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FsVaultTest {
    private static final String TEST_VAULT = "test-vault.properties";

    private FsVault vault;

    @Test
    void verifyResolution() {
        assertEquals("secretvalue1", vault.resolveSecret("secret1"));
        assertEquals("secretvalue2", vault.resolveSecret("secret2"));
    }

    @BeforeEach
    void setUp() throws URISyntaxException {
        var uri = getClass().getClassLoader().getResource(TEST_VAULT).toURI();
        vault = new FsVault(Paths.get(uri), false);
    }
}
