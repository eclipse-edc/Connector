/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.security.fs;

import com.microsoft.dagx.spi.DagxSetting;

import static com.microsoft.dagx.common.ConfigurationFunctions.propOrEnv;

public final class FsConfiguration {

    @DagxSetting
    final static String VAULT_LOCATION = propOrEnv("dagx.vault", "dagx-vault.properties");

    @DagxSetting
    final static String KEYSTORE_LOCATION = propOrEnv("dagx.keystore", "dagx-keystore.jks");

    @DagxSetting
    final static String KEYSTORE_PASSWORD = propOrEnv("dagx.keystore.password", "test123");

    @DagxSetting
    final static boolean PERSISTENT_VAULT = Boolean.parseBoolean(propOrEnv("dagx.vault.persistent", "true"));


    private FsConfiguration() {
    }
}
