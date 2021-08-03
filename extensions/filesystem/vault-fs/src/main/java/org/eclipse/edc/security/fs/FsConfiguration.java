/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.security.fs;

import org.eclipse.edc.spi.EdcSetting;

import static org.eclipse.edc.common.ConfigurationFunctions.propOrEnv;

public final class FsConfiguration {

    @EdcSetting
    final static String VAULT_LOCATION = propOrEnv("edc.vault", "edc-vault.properties");

    @EdcSetting
    final static String KEYSTORE_LOCATION = propOrEnv("edc.keystore", "edc-keystore.jks");

    @EdcSetting
    final static String KEYSTORE_PASSWORD = propOrEnv("edc.keystore.password", "test123");

    @EdcSetting
    final static boolean PERSISTENT_VAULT = Boolean.parseBoolean(propOrEnv("edc.vault.persistent", "true"));


    private FsConfiguration() {
    }
}
