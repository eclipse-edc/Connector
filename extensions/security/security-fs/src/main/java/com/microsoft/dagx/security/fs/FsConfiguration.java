package com.microsoft.dagx.security.fs;

import com.microsoft.dagx.spi.DagxSetting;

import static com.microsoft.dagx.spi.util.ConfigurationFunctions.propOrEnv;

public final class FsConfiguration {

    @DagxSetting
    final static String VAULT_LOCATION = propOrEnv("dagx.vault", "dagx-vault.properties");

    @DagxSetting
    final static String KEYSTORE_LOCATION = propOrEnv("dagx.keystore", "dagx-keystore.jks");

    @DagxSetting
    final static String KEYSTORE_PASSWORD = propOrEnv("dagx.keystore.password", "test123");


    private FsConfiguration() {
    }
}
