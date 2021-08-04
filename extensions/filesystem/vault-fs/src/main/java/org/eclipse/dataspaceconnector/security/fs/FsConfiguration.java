/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.security.fs;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

import static org.eclipse.dataspaceconnector.common.ConfigurationFunctions.propOrEnv;

public final class FsConfiguration {

    @EdcSetting
    final static String VAULT_LOCATION = propOrEnv("dataspaceconnector.vault", "dataspaceconnector-vault.properties");

    @EdcSetting
    final static String KEYSTORE_LOCATION = propOrEnv("dataspaceconnector.keystore", "dataspaceconnector-keystore.jks");

    @EdcSetting
    final static String KEYSTORE_PASSWORD = propOrEnv("dataspaceconnector.keystore.password", "test123");

    @EdcSetting
    final static boolean PERSISTENT_VAULT = Boolean.parseBoolean(propOrEnv("dataspaceconnector.vault.persistent", "true"));


    private FsConfiguration() {
    }
}
