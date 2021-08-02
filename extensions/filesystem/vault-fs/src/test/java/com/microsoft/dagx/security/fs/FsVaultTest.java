/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.security.fs;

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
        @SuppressWarnings("ConstantConditions") var uri = getClass().getClassLoader().getResource(TEST_VAULT).toURI();
        vault = new FsVault(Paths.get(uri), false);
    }
}
