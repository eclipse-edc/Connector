/*
 *  Copyright (c) 2024 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH - introduce integration test against a more advanced, commercial vault
 *
 */
package org.eclipse.edc.vault.hashicorp.client;

/**
 * Integration test against the original (non-FOSS) vault implementation that is more advanced than the
 * FOSS variants.
 */
public class HashicorpVaultClientOriginalIntegrationTest extends HashicorpVaultClientIntegrationTest {

    /**
     * overrides the container image
     * @return a more current version of vault
     */
    @Override
    protected String getVaultImage() {
        return "hashicorp/vault:1.17.3";
    }
}
